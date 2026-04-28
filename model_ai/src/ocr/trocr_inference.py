from __future__ import annotations

import os
import warnings
from pathlib import Path

import torch

# Keep TrOCR pipeline on PyTorch only to avoid TensorFlow import side effects.
os.environ.setdefault("TRANSFORMERS_NO_TF", "1")
os.environ.setdefault("TRANSFORMERS_NO_FLAX", "1")
os.environ.setdefault("USE_TF", "0")
os.environ.setdefault("USE_FLAX", "0")

from transformers import TrOCRProcessor, VisionEncoderDecoderModel

from .trocr_data import load_ocr_image_rgb


def _get_available_device(requested_device: str = "cuda") -> str:
    """Detect available device, falling back to CPU if needed."""
    if requested_device == "cpu":
        return "cpu"
    
    if not torch.cuda.is_available():
        warnings.warn("CUDA not available, falling back to CPU", UserWarning)
        return "cpu"
    
    try:
        # Try to allocate a small tensor to verify CUDA actually works
        torch.empty(1, device="cuda")
        return "cuda"
    except (AssertionError, RuntimeError) as e:
        warnings.warn(
            f"CUDA not functional ({e}), falling back to CPU. "
            "Install CUDA-enabled PyTorch: pip install torch torchvision --index-url https://download.pytorch.org/whl/cu118",
            UserWarning,
        )
        return "cpu"


def predict_batch_trocr(
    model_path: str | Path,
    image_paths: list[str | Path],
    device: str = "cuda",
    max_new_tokens: int = 128,
) -> dict[str, str]:
    """
    Generate predictions for a batch of images using TrOCR.
    """
    model_path = Path(model_path)
    available_device = _get_available_device(device)
    device_obj = torch.device(available_device)

    # Load model and processor
    model = VisionEncoderDecoderModel.from_pretrained(model_path)
    processor = TrOCRProcessor.from_pretrained(model_path)

    model = model.to(device_obj)
    model.eval()

    predictions = {}

    with torch.no_grad():
        for image_path in image_paths:
            # Load and preprocess image
            image = load_ocr_image_rgb(image_path)
            pixel_values = processor(images=image, return_tensors="pt").pixel_values
            pixel_values = pixel_values.to(device_obj)

            # Generate prediction
            generated_ids = model.generate(
                pixel_values,
                max_new_tokens=max_new_tokens,
                num_beams=1,  # Greedy decoding for speed
            )

            # Decode to text
            predicted_text = processor.batch_decode(
                generated_ids,
                skip_special_tokens=True,
            )[0]

            predictions[str(image_path)] = predicted_text

    return predictions


def predict_text_trocr(
    model_path: str | Path,
    image_path: str | Path,
    device: str = "cuda",
    max_new_tokens: int = 128,
) -> str:
    """
    Generate prediction for a single image using TrOCR.
    """
    result = predict_batch_trocr(
        model_path=model_path,
        image_paths=[image_path],
        device=device,
        max_new_tokens=max_new_tokens,
    )
    return result[str(image_path)]
