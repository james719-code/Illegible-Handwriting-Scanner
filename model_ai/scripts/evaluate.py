from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
SRC_ROOT = PROJECT_ROOT / "src"
if str(SRC_ROOT) not in sys.path:
    sys.path.insert(0, str(SRC_ROOT))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Evaluate a paired OCR model on image + transcription samples.")
    parser.add_argument("--model", required=True, help="Path to a trained OCR .keras model.")
    parser.add_argument(
        "--dataset-root",
        "--dataset-dir",
        dest="dataset_dir",
        default=str(PROJECT_ROOT / "data" / "raw"),
    )
    parser.add_argument(
        "--charset",
        "--vocabulary",
        dest="vocabulary",
        help="Optional path to the saved vocabulary JSON. Defaults to the model folder vocabulary.json",
    )
    parser.add_argument("--config", help="Optional path to train_config.json. Defaults to the model folder train_config.json")
    parser.add_argument("--image-width", type=int)
    parser.add_argument("--image-height", type=int)
    parser.add_argument("--max-text-length", type=int)
    parser.add_argument("--max-samples", type=int)
    parser.add_argument("--lowercase", action="store_true")
    parser.add_argument("--output")
    parser.add_argument("--save-predictions", action="store_true", help="Save every prediction to a JSON file.")
    parser.add_argument("--benchmark-name", help="Optional label for this evaluated run.")
    parser.add_argument(
        "--benchmark-group",
        help="Optional group used to compare runs. Defaults to the resolved dataset path.",
    )
    parser.add_argument(
        "--benchmark-history",
        help="Optional benchmark history JSON path. Defaults to benchmark_history.json beside the evaluation report.",
    )
    parser.add_argument("--skip-benchmark", action="store_true", help="Do not append this run to benchmark history.")
    return parser.parse_args()


def _load_training_defaults(model_path: Path, config_override: str | None) -> dict[str, object]:
    config_path = Path(config_override) if config_override else model_path.parent / "train_config.json"
    if not config_path.exists():
        return {}
    return json.loads(config_path.read_text(encoding="utf-8"))


def main() -> None:
    import tensorflow as tf

    from evaluation.benchmark import (
        create_ocr_benchmark_entry,
        format_ocr_benchmark_summary,
        load_benchmark_history,
        save_benchmark_csv,
        save_benchmark_history,
        summarize_ocr_benchmark,
    )
    from evaluation.metrics import ocr_report
    from evaluation.report import save_report
    from ocr.data import load_ocr_arrays
    from ocr.text import decode_ctc_prediction_batch, decode_prediction_batch, load_vocabulary

    args = parse_args()
    model_path = Path(args.model)
    defaults = _load_training_defaults(model_path, args.config)

    image_width = args.image_width or int(defaults.get("image_width", 256))
    image_height = args.image_height or int(defaults.get("image_height", 64))
    max_text_length = args.max_text_length or defaults.get("max_text_length")

    vocabulary_path = Path(args.vocabulary) if args.vocabulary else model_path.parent / "vocabulary.json"
    vocabulary_payload = load_vocabulary(vocabulary_path)
    vocabulary = list(vocabulary_payload["tokens"])

    image_width = args.image_width or int(vocabulary_payload.get("image_width", image_width))
    image_height = args.image_height or int(vocabulary_payload.get("image_height", image_height))
    max_text_length = args.max_text_length or vocabulary_payload.get("max_text_length") or max_text_length
    if max_text_length is None:
        raise SystemExit(
            "Could not determine max text length. Pass --max-text-length or provide vocabulary.json/train_config.json.",
        )

    images, _, texts, paths, _, _ = load_ocr_arrays(
        dataset_root=args.dataset_dir,
        image_size=(image_width, image_height),
        vocabulary=vocabulary,
        max_text_length=int(max_text_length),
        lowercase=args.lowercase,
        max_samples=args.max_samples,
    )

    model = tf.keras.models.load_model(model_path, compile=False)
    predictions = model.predict(images, verbose=0)
    decoder = str(vocabulary_payload.get("decoder", "argmax"))
    if decoder == "ctc":
        decoded = decode_ctc_prediction_batch(
            predictions,
            vocabulary,
            blank_index=int(vocabulary_payload.get("blank_index", len(vocabulary))),
        )
    else:
        decoded = decode_prediction_batch(predictions, vocabulary)
    report = ocr_report(texts, decoded)
    report["image_width"] = image_width
    report["image_height"] = image_height
    report["max_text_length"] = int(max_text_length)
    report["vocabulary_path"] = str(vocabulary_path)
    report["preview"] = [
        {
            "image_path": paths[index],
            "reference": texts[index],
            "prediction": decoded[index],
        }
        for index in range(min(5, len(paths)))
    ]

    output_path = Path(args.output) if args.output else PROJECT_ROOT / "models" / "exported" / "ocr" / "evaluation.json"
    benchmark_history_path = Path(args.benchmark_history) if args.benchmark_history else output_path.with_name("benchmark_history.json")

    if not args.skip_benchmark:
        benchmark_entry = create_ocr_benchmark_entry(
            report,
            model_path=model_path,
            dataset_root=args.dataset_dir,
            report_path=output_path,
            benchmark_name=args.benchmark_name,
            benchmark_group=args.benchmark_group,
        )
        benchmark_history = load_benchmark_history(benchmark_history_path)
        benchmark_history.append(benchmark_entry)
        save_benchmark_history(benchmark_history, benchmark_history_path)
        benchmark_csv_path = save_benchmark_csv(benchmark_history, benchmark_history_path.with_suffix(".csv"))
        benchmark_summary = summarize_ocr_benchmark(benchmark_history, benchmark_entry)
        benchmark_summary["history_path"] = str(benchmark_history_path.resolve())
        benchmark_summary["csv_path"] = str(benchmark_csv_path.resolve())
        report["benchmark"] = benchmark_summary

    save_report(report, output_path)

    if args.save_predictions:
        predictions_path = output_path.with_name(f"{output_path.stem}_predictions.json")
        predictions_path.write_text(
            json.dumps(
                [
                    {
                        "image_path": paths[index],
                        "reference": texts[index],
                        "prediction": decoded[index],
                    }
                    for index in range(len(paths))
                ],
                indent=2,
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )

    print(f"Saved evaluation report to: {output_path}")
    if not args.skip_benchmark:
        print(f"Updated benchmark history: {benchmark_history_path}")
        for line in format_ocr_benchmark_summary(report["benchmark"], benchmark_entry):
            print(line)


if __name__ == "__main__":
    main()
