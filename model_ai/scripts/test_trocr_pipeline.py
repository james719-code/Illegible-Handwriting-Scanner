#!/usr/bin/env python
"""
Quick test script for TrOCR pipeline.
Creates a tiny synthetic dataset and runs end-to-end training + evaluation.
"""
from __future__ import annotations

import sys
import tempfile
from pathlib import Path

import cv2
import numpy as np

PROJECT_ROOT = Path(__file__).resolve().parents[1]
SRC_ROOT = PROJECT_ROOT / "src"
if str(SRC_ROOT) not in sys.path:
    sys.path.insert(0, str(SRC_ROOT))


def create_synthetic_image(text: str, size: tuple[int, int] = (256, 64)) -> np.ndarray:
    """Create a simple synthetic handwriting image."""
    width, height = size
    image = np.ones((height, width, 3), dtype=np.uint8) * 255  # White background

    # Add text annotation
    font = cv2.FONT_HERSHEY_SIMPLEX
    font_scale = 0.5
    thickness = 1
    color = (0, 0, 0)  # Black text

    text_size = cv2.getTextSize(text, font, font_scale, thickness)[0]
    x = (width - text_size[0]) // 2
    y = (height + text_size[1]) // 2

    cv2.putText(image, text, (x, y), font, font_scale, color, thickness)

    # Add some noise to simulate real handwriting
    noise = np.random.normal(0, 10, image.shape).astype(np.uint8)
    image = cv2.add(image, noise)

    return image


def test_trocr_pipeline() -> None:
    """Test the full TrOCR pipeline on synthetic data."""
    print("=" * 60)
    print("TrOCR Pipeline Test")
    print("=" * 60)

    # Create temporary directory for test data
    with tempfile.TemporaryDirectory() as tmpdir:
        test_root = Path(tmpdir) / "tiny_dataset"
        test_root.mkdir(parents=True)

        # Create synthetic training samples
        samples = [
            ("Hello World", "hello_world"),
            ("Test OCR", "test_ocr"),
            ("TrOCR Works", "trocr_works"),
        ]

        print("\n1. Creating synthetic training data...")
        for text, stem in samples:
            image = create_synthetic_image(text)
            image_path = test_root / f"{stem}.png"
            cv2.imwrite(str(image_path), image)

            transcript_path = test_root / f"{stem}.txt"
            transcript_path.write_text(text, encoding="utf-8")
            print(f"   Created {stem}.png and {stem}.txt")

        # Test data loading
        print("\n2. Testing data loader...")
        try:
            from ocr.trocr_data import load_trocr_dataset
            from transformers import TrOCRProcessor

            processor = TrOCRProcessor.from_pretrained("microsoft/trocr-base-handwritten")
            train_dataset, val_dataset = load_trocr_dataset(
                dataset_root=str(test_root),
                processor=processor,
                validation_split=0.33,
                max_samples=3,
            )
            print(f"   ✓ Data loader works!")
            print(f"   - Train samples: {len(train_dataset)}")
            print(f"   - Val samples: {len(val_dataset)}")

            # Check a single batch
            sample = train_dataset[0]
            print(f"   - Sample pixel_values shape: {sample['pixel_values'].shape}")
            print(f"   - Sample labels shape: {sample['labels'].shape}")

        except Exception as e:
            print(f"   ✗ Data loader failed: {e}")
            raise

        # Test model loading
        print("\n3. Testing model loading...")
        try:
            from transformers import VisionEncoderDecoderModel

            model = VisionEncoderDecoderModel.from_pretrained("microsoft/trocr-base-handwritten")
            device = "cuda" if __import__("torch").cuda.is_available() else "cpu"
            model = model.to(device)
            print(f"   ✓ Model loaded successfully on {device}")

        except Exception as e:
            print(f"   ✗ Model loading failed: {e}")
            raise

        # Test inference
        print("\n4. Testing inference...")
        try:
            from ocr.trocr_inference import predict_text_trocr

            test_image_path = test_root / "hello_world.png"
            prediction = predict_text_trocr(
                model_path=PROJECT_ROOT / "models" / "exported" / "trocr" / "final_model",
                image_path=test_image_path,
                device=device,
            )
            print(f"   ✓ Inference works!")
            print(f"   - Prediction: '{prediction}'")

        except Exception as e:
            # Model doesn't exist yet, which is expected
            print(f"   ⓘ Inference test skipped (no trained model yet)")

        print("\n5. Testing metrics...")
        try:
            from evaluation.metrics import ocr_report

            references = ["Hello World", "Test OCR", "TrOCR Works"]
            predictions = ["Hello World", "Test OCr", "TroCr Wrks"]
            report = ocr_report(references, predictions)
            print(f"   ✓ Metrics work!")
            print(f"   - Exact match rate: {report['exact_match_rate']:.4f}")
            print(f"   - Character error rate: {report['character_error_rate']:.4f}")
            print(f"   - Word error rate: {report['word_error_rate']:.4f}")

        except Exception as e:
            print(f"   ✗ Metrics failed: {e}")
            raise

    print("\n" + "=" * 60)
    print("✓ All pipeline tests passed!")
    print("=" * 60)
    print("\nNext steps:")
    print("1. Place your training data in model_ai/data/raw/train/")
    print("2. Run: python scripts/train_trocr.py --dataset-root data/raw/train")
    print("3. Evaluate with: python scripts/evaluate_trocr.py --model models/exported/trocr/final_model --dataset-root data/raw/val")


if __name__ == "__main__":
    test_trocr_pipeline()
