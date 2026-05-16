from __future__ import annotations

import argparse
import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
SRC_ROOT = PROJECT_ROOT / "src"
if str(SRC_ROOT) not in sys.path:
    sys.path.insert(0, str(SRC_ROOT))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Fine-tune TrOCR for handwritten OCR from image + transcription pairs.")
    parser.add_argument(
        "--dataset-root",
        "--dataset-dir",
        dest="dataset_dir",
        default=str(PROJECT_ROOT / "data" / "raw"),
        help="Root folder that contains paired image files and same-stem .txt transcripts.",
    )
    parser.add_argument("--output-root", default=str(PROJECT_ROOT / "models"))
    parser.add_argument("--batch-size", type=int, default=8)
    parser.add_argument("--epochs", type=int, default=25)
    parser.add_argument("--validation-split", type=float, default=0.2)
    parser.add_argument("--learning-rate", type=float, default=1e-5)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--max-samples", type=int)
    parser.add_argument(
        "--max-text-length",
        type=int,
        help="Optional transcript-length filter. Keeps only samples whose transcription is at most this many characters.",
    )
    parser.add_argument("--lowercase", action="store_true")
    parser.add_argument("--device", choices=["auto", "cuda", "cpu"], default="auto")
    parser.add_argument("--model-name", default="microsoft/trocr-base-handwritten")
    parser.add_argument("--freeze-encoder", action="store_true")
    parser.add_argument("--num-workers", type=int, default=0)
    parser.add_argument("--no-mixed-precision", action="store_true")
    parser.add_argument("--gradient-clip-norm", type=float, default=1.0)
    parser.add_argument("--no-pin-memory", action="store_true")
    parser.add_argument("--no-persistent-workers", action="store_true")
    return parser.parse_args()


def main() -> None:
    args = parse_args()

    from ocr.trocr_train import TrOCRTrainConfig, TrOCRTrainer

    config = TrOCRTrainConfig(
        dataset_dir=args.dataset_dir,
        output_root=args.output_root,
        batch_size=args.batch_size,
        epochs=args.epochs,
        validation_split=args.validation_split,
        learning_rate=args.learning_rate,
        seed=args.seed,
        max_samples=args.max_samples,
        max_text_length=args.max_text_length,
        lowercase=args.lowercase,
        device=args.device,
        model_name=args.model_name,
        freeze_encoder=args.freeze_encoder,
        num_workers=args.num_workers,
        mixed_precision=not args.no_mixed_precision,
        gradient_clip_norm=args.gradient_clip_norm,
        pin_memory=not args.no_pin_memory,
        persistent_workers=not args.no_persistent_workers,
    )

    print(f"Starting TrOCR training with config:")
    print(f"  Dataset: {args.dataset_dir}")
    print(f"  Model: {args.model_name}")
    print(f"  Requested device: {args.device}")
    print(f"  Batch size: {args.batch_size}")
    print(f"  Epochs: {args.epochs}")
    print(f"  Learning rate: {args.learning_rate}")
    print(f"  Max text length filter: {args.max_text_length if args.max_text_length is not None else 'none'}")
    print(f"  Mixed precision: {not args.no_mixed_precision}")

    trainer = TrOCRTrainer(config)
    
    print(f"  Actual device: {trainer.device}")
    if str(trainer.device) != args.device:
        print(f"  (Note: device changed from {args.device} to {trainer.device})")
    
    artifacts = trainer.fit()

    print(f"\nTraining complete. Saved artifacts to:")
    for key, path in artifacts.items():
        print(f"  {key}: {path}")


if __name__ == "__main__":
    main()
