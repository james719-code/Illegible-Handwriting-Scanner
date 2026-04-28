#!/usr/bin/env python
"""
Compare OCR baseline vs TrOCR on a validation dataset.
Generates side-by-side metrics and a detailed comparison report.
"""
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
    parser = argparse.ArgumentParser(description="Compare baseline OCR model vs TrOCR on validation set.")
    parser.add_argument(
        "--dataset-root",
        "--dataset-dir",
        dest="dataset_dir",
        default=str(PROJECT_ROOT / "data" / "raw" / "val"),
        help="Validation dataset root.",
    )
    parser.add_argument(
        "--baseline-model",
        default=str(PROJECT_ROOT / "models" / "exported" / "ocr" / "final_model.keras"),
        help="Path to baseline Keras model.",
    )
    parser.add_argument(
        "--trocr-model",
        default=str(PROJECT_ROOT / "models" / "exported" / "trocr" / "final_model"),
        help="Path to TrOCR model directory.",
    )
    parser.add_argument("--max-samples", type=int)
    parser.add_argument("--output")
    parser.add_argument("--device", choices=["cuda", "cpu"], default="cuda")
    return parser.parse_args()


def load_baseline_predictions(
    model_path: str | Path,
    dataset_dir: str | Path,
    image_size: tuple[int, int] = (256, 64),
    max_samples: int | None = None,
) -> tuple[list[str], list[str], list[str]]:
    """Load predictions from baseline Keras model."""
    try:
        import tensorflow as tf
        from ocr.data import load_ocr_arrays
        from ocr.text import decode_prediction_batch, load_vocabulary

        model_path = Path(model_path)
        vocabulary_path = model_path.parent / "vocabulary.json"

        if not vocabulary_path.exists():
            raise FileNotFoundError(f"Vocabulary not found: {vocabulary_path}")

        vocabulary_payload = load_vocabulary(vocabulary_path)
        vocabulary = list(vocabulary_payload["tokens"])

        images, _, texts, paths, _, _ = load_ocr_arrays(
            dataset_root=dataset_dir,
            image_size=image_size,
            vocabulary=vocabulary,
            max_text_length=int(vocabulary_payload["max_text_length"]),
            max_samples=max_samples,
        )

        model = tf.keras.models.load_model(model_path, compile=False)
        predictions = model.predict(images, verbose=0)
        decoded = decode_prediction_batch(predictions, vocabulary)

        return texts, decoded, paths

    except Exception as e:
        print(f"Error loading baseline predictions: {e}")
        raise


def load_trocr_predictions(
    model_path: str | Path,
    dataset_dir: str | Path,
    device: str = "cuda",
    max_samples: int | None = None,
) -> tuple[list[str], list[str], list[str]]:
    """Load predictions from TrOCR model."""
    try:
        from ocr.data import load_ocr_prediction_batch
        from ocr.trocr_inference import predict_batch_trocr

        batch = load_ocr_prediction_batch(
            dataset_root=dataset_dir,
            image_size=(384, 384),
            max_samples=max_samples,
        )

        predictions_dict = predict_batch_trocr(
            model_path=model_path,
            image_paths=batch.image_paths,
            device=device,
            max_new_tokens=128,
        )

        decoded = [predictions_dict[str(path)] for path in batch.image_paths]
        return batch.texts, decoded, batch.image_paths

    except Exception as e:
        print(f"Error loading TrOCR predictions: {e}")
        raise


def compute_report(references: list[str], predictions: list[str]) -> dict:
    """Compute OCR metrics."""
    from evaluation.metrics import ocr_report

    return ocr_report(references, predictions)


def main() -> None:
    args = parse_args()

    print("=" * 70)
    print("OCR Model Comparison: Baseline vs TrOCR")
    print("=" * 70)

    baseline_model = Path(args.baseline_model)
    trocr_model = Path(args.trocr_model)

    # Check model existence
    if not baseline_model.exists():
        print(f"WARNING: Baseline model not found at {baseline_model}")
        print("  Run: python scripts/train_ocr.py --dataset-root data/raw/train")
        baseline_report = None
    else:
        print(f"\n1. Loading baseline predictions from {baseline_model.name}...")
        try:
            baseline_refs, baseline_preds, baseline_paths = load_baseline_predictions(
                model_path=baseline_model,
                dataset_dir=args.dataset_dir,
                max_samples=args.max_samples,
            )
            baseline_report = compute_report(baseline_refs, baseline_preds)
            print(f"   Loaded {len(baseline_refs)} samples")
        except Exception as e:
            print(f"   ERROR: {e}")
            baseline_report = None

    if not trocr_model.exists():
        print(f"\nWARNING: TrOCR model not found at {trocr_model}")
        print("  Run: python scripts/train_trocr.py --dataset-root data/raw/train")
        trocr_report = None
    else:
        print(f"\n2. Loading TrOCR predictions from {trocr_model.name}...")
        try:
            trocr_refs, trocr_preds, trocr_paths = load_trocr_predictions(
                model_path=trocr_model,
                dataset_dir=args.dataset_dir,
                device=args.device,
                max_samples=args.max_samples,
            )
            trocr_report = compute_report(trocr_refs, trocr_preds)
            print(f"   Loaded {len(trocr_refs)} samples")
        except Exception as e:
            print(f"   ERROR: {e}")
            trocr_report = None

    # Display comparison
    print("\n" + "=" * 70)
    print("METRICS COMPARISON")
    print("=" * 70)

    if baseline_report and trocr_report:
        print(f"\n{'Metric':<30} {'Baseline':<20} {'TrOCR':<20} {'Delta':<15}")
        print("-" * 85)

        metrics = [
            ("Exact Match Rate", "exact_match_rate"),
            ("Character Error Rate", "character_error_rate"),
            ("Word Error Rate", "word_error_rate"),
        ]

        for metric_name, metric_key in metrics:
            baseline_val = baseline_report[metric_key]
            trocr_val = trocr_report[metric_key]
            delta = trocr_val - baseline_val

            # Better direction depends on metric
            if metric_key == "exact_match_rate":
                better = "↑" if delta > 0 else "↓"
            else:
                better = "↓" if delta < 0 else "↑"

            print(
                f"{metric_name:<30} {baseline_val:>18.4f}  {trocr_val:>18.4f}  {delta:>+.4f} {better}"
            )

        # Summary
        print("\n" + "-" * 85)
        print("SUMMARY:")
        baseline_exact = baseline_report["exact_match_rate"]
        trocr_exact = trocr_report["exact_match_rate"]
        improvement = (trocr_exact - baseline_exact) / max(baseline_exact, 0.0001) * 100

        if trocr_exact > baseline_exact:
            print(f"✓ TrOCR WINS: +{improvement:.1f}% exact match rate improvement")
        elif trocr_exact < baseline_exact:
            print(f"✗ Baseline wins: TrOCR is {-improvement:.1f}% worse")
        else:
            print("= Tie: both models perform equally")

        baseline_cer = baseline_report["character_error_rate"]
        trocr_cer = trocr_report["character_error_rate"]
        cer_improvement = (baseline_cer - trocr_cer) / max(baseline_cer, 0.0001) * 100

        if trocr_cer < baseline_cer:
            print(f"✓ TrOCR has {cer_improvement:.1f}% lower character error rate")
        else:
            print(f"✗ TrOCR has {-cer_improvement:.1f}% higher character error rate")

    elif baseline_report:
        print("\nBaseline metrics (TrOCR not available):")
        print(f"  Exact Match Rate: {baseline_report['exact_match_rate']:.4f}")
        print(f"  Character Error Rate: {baseline_report['character_error_rate']:.4f}")
        print(f"  Word Error Rate: {baseline_report['word_error_rate']:.4f}")

    elif trocr_report:
        print("\nTrOCR metrics (Baseline not available):")
        print(f"  Exact Match Rate: {trocr_report['exact_match_rate']:.4f}")
        print(f"  Character Error Rate: {trocr_report['character_error_rate']:.4f}")
        print(f"  Word Error Rate: {trocr_report['word_error_rate']:.4f}")

    else:
        print("\nNo models available for comparison.")

    # Save detailed report
    if args.output:
        output_path = Path(args.output)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        report = {
            "baseline": baseline_report,
            "trocr": trocr_report,
        }
        output_path.write_text(json.dumps(report, indent=2), encoding="utf-8")
        print(f"\nDetailed report saved to: {output_path}")

    print("\n" + "=" * 70)


if __name__ == "__main__":
    main()
