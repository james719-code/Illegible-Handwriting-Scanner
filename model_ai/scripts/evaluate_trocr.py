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
    parser = argparse.ArgumentParser(description="Evaluate a fine-tuned TrOCR model on image + transcription samples.")
    parser.add_argument("--model", required=True, help="Path to a TrOCR model directory with config.json.")
    parser.add_argument(
        "--dataset-root",
        "--dataset-dir",
        dest="dataset_dir",
        default=str(PROJECT_ROOT / "data" / "raw"),
    )
    parser.add_argument("--max-samples", type=int)
    parser.add_argument(
        "--max-text-length",
        type=int,
        help="Optional transcript-length filter. Keeps only samples whose transcription is at most this many characters.",
    )
    parser.add_argument("--lowercase", action="store_true")
    parser.add_argument("--output")
    parser.add_argument("--device", choices=["cuda", "cpu"], default="cuda")
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


def main() -> None:
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
    from ocr.data import load_ocr_prediction_batch
    from ocr.trocr_inference import predict_batch_trocr

    args = parse_args()
    model_path = Path(args.model)

    # Load validation data
    batch = load_ocr_prediction_batch(
        dataset_root=args.dataset_dir,
        image_size=(384, 384),  # TrOCR processor handles resizing
        lowercase=args.lowercase,
        max_samples=args.max_samples,
        max_transcription_length=args.max_text_length,
    )

    # Run predictions
    print(f"Evaluating {len(batch.image_paths)} samples...")
    predictions_dict = predict_batch_trocr(
        model_path=model_path,
        image_paths=batch.image_paths,
        device=args.device,
        max_new_tokens=128,
    )

    # Extract predictions in order
    decoded = [predictions_dict[str(path)] for path in batch.image_paths]

    # Compute metrics
    report = ocr_report(batch.texts, decoded)
    report["image_width"] = 384
    report["image_height"] = 384
    report["model_name"] = "trocr"
    report["model_path"] = str(model_path)
    report["max_text_length"] = args.max_text_length
    report["preview"] = [
        {
            "image_path": batch.image_paths[index],
            "reference": batch.texts[index],
            "prediction": decoded[index],
        }
        for index in range(min(5, len(batch.image_paths)))
    ]

    output_path = Path(args.output) if args.output else PROJECT_ROOT / "models" / "exported" / "trocr" / "evaluation.json"
    output_path.parent.mkdir(parents=True, exist_ok=True)
    benchmark_history_path = Path(args.benchmark_history) if args.benchmark_history else output_path.with_name("benchmark_history.json")

    if not args.skip_benchmark:
        benchmark_entry = create_ocr_benchmark_entry(
            report,
            model_path=model_path,
            dataset_root=args.dataset_dir,
            report_path=output_path,
            benchmark_name=args.benchmark_name or "trocr_eval",
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
                        "image_path": batch.image_paths[index],
                        "reference": batch.texts[index],
                        "prediction": decoded[index],
                    }
                    for index in range(len(batch.image_paths))
                ],
                indent=2,
                ensure_ascii=False,
            ),
            encoding="utf-8",
        )

    print(f"Saved evaluation report to: {output_path}")
    print(f"\nMetrics:")
    print(f"  Exact Match Rate: {report['exact_match_rate']:.4f}")
    print(f"  Character Error Rate: {report['character_error_rate']:.4f}")
    print(f"  Word Error Rate: {report['word_error_rate']:.4f}")

    if not args.skip_benchmark:
        print(f"\nUpdated benchmark history: {benchmark_history_path}")
        for line in format_ocr_benchmark_summary(report["benchmark"], benchmark_entry):
            print(line)


if __name__ == "__main__":
    main()
