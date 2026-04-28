from __future__ import annotations

import argparse
import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
SRC_ROOT = PROJECT_ROOT / "src"
if str(SRC_ROOT) not in sys.path:
    sys.path.insert(0, str(SRC_ROOT))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Train an OCR model from image + transcription text pairs.")
    parser.add_argument(
        "--dataset-root",
        "--dataset-dir",
        dest="dataset_dir",
        default=str(PROJECT_ROOT / "data" / "raw"),
        help="Root folder that contains paired image files and same-stem .txt transcripts.",
    )
    parser.add_argument("--output-root", default=str(PROJECT_ROOT / "models"))
    parser.add_argument("--image-width", type=int, default=256)
    parser.add_argument("--image-height", type=int, default=64)
    parser.add_argument("--batch-size", type=int, default=8)
    parser.add_argument("--epochs", type=int, default=25)
    parser.add_argument("--validation-split", type=float, default=0.2)
    parser.add_argument("--learning-rate", type=float, default=1e-3)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--max-text-length", type=int)
    parser.add_argument("--max-samples", type=int)
    parser.add_argument("--lowercase", action="store_true")
    return parser.parse_args()


def main() -> None:
    args = parse_args()

    from ocr.train import OCRTrainConfig, OCRTrainer

    config = OCRTrainConfig(
        dataset_dir=args.dataset_dir,
        output_root=args.output_root,
        image_width=args.image_width,
        image_height=args.image_height,
        batch_size=args.batch_size,
        epochs=args.epochs,
        validation_split=args.validation_split,
        learning_rate=args.learning_rate,
        seed=args.seed,
        max_text_length=args.max_text_length,
        max_samples=args.max_samples,
        lowercase=args.lowercase,
    )
    artifacts = OCRTrainer(config).fit()
    print(f"Saved OCR artifacts to: {artifacts['model'].parent}")


if __name__ == "__main__":
    main()
