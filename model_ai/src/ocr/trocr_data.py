from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path
from typing import Optional

import cv2
import numpy as np
import torch
from torch.utils.data import DataLoader, Dataset

# Keep TrOCR pipeline on PyTorch only to avoid TensorFlow import side effects.
os.environ.setdefault("TRANSFORMERS_NO_TF", "1")
os.environ.setdefault("TRANSFORMERS_NO_FLAX", "1")
os.environ.setdefault("USE_TF", "0")
os.environ.setdefault("USE_FLAX", "0")

from transformers import TrOCRProcessor

from .data import discover_ocr_pairs
from .text import build_vocabulary, normalize_transcription


@dataclass(slots=True)
class TrOCRSample:
    image_path: Path
    transcription: str
    image_array: np.ndarray


def load_ocr_image_rgb(
    image_path: str | Path,
) -> np.ndarray:
    """Load image as RGB for TrOCR (which expects 3-channel)."""
    image = cv2.imread(str(image_path), cv2.IMREAD_COLOR)
    if image is None:
        raise FileNotFoundError(f"Unable to read image: {image_path}")
    # Convert BGR to RGB
    image_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    return image_rgb


def _filter_pairs_by_length(
    pairs: list,
    max_text_length: int | None,
) -> list:
    if max_text_length is None:
        return pairs
    return [pair for pair in pairs if len(pair.transcription) <= max_text_length]


def augment_image(
    image: np.ndarray,
    apply_augmentation: bool = True,
) -> np.ndarray:
    """
    Apply handwriting-robust augmentation to training images.
    Includes rotation, blur, brightness, and slight perspective changes.
    """
    if not apply_augmentation:
        return image

    try:
        import albumentations as A
    except ImportError:
        # Fallback if albumentations is not available
        return image

    # Compose augmentations suitable for handwritten text
    transform = A.Compose(
        [
            A.Rotate(limit=15, border_mode=cv2.BORDER_CONSTANT, p=0.5),
            A.GaussianBlur(sigma_limit=(0.1, 2.0), p=0.3),
            A.RandomBrightnessContrast(brightness_limit=0.2, contrast_limit=0.2, p=0.3),
            A.ElasticTransform(alpha=30, sigma=5, p=0.2),
            A.Perspective(scale=(0.05, 0.1), p=0.2),
            A.GaussNoise(p=0.2),
        ],
        is_check_shapes=False,
    )

    augmented = transform(image=image)
    return augmented["image"]


class TrOCRDataset(Dataset):
    """PyTorch Dataset for TrOCR fine-tuning from image+text pairs."""

    def __init__(
        self,
        image_paths: list[str | Path],
        transcriptions: list[str],
        processor: TrOCRProcessor,
        augment: bool = False,
    ):
        self.image_paths = [Path(p) for p in image_paths]
        self.transcriptions = transcriptions
        self.processor = processor
        self.augment = augment

        if len(self.image_paths) != len(self.transcriptions):
            raise ValueError("Number of images must match number of transcriptions")

    def __len__(self) -> int:
        return len(self.image_paths)

    def __getitem__(self, idx: int) -> dict[str, torch.Tensor]:
        image_path = self.image_paths[idx]
        transcription = self.transcriptions[idx]

        # Load image as RGB
        image = load_ocr_image_rgb(image_path)

        # Apply augmentation during training
        if self.augment:
            image = augment_image(image, apply_augmentation=True)

        # Process image and text using TrOCRProcessor
        pixel_values = self.processor(images=image, return_tensors="pt").pixel_values
        labels = self.processor.tokenizer(
            transcription,
            return_tensors="pt",
            padding="max_length",
            max_length=128,
            truncation=True,
        ).input_ids

        # Ignore padded positions in the loss.
        labels = labels.clone()
        labels[labels == self.processor.tokenizer.pad_token_id] = -100

        # Remove batch dimension (DataLoader will add it back)
        return {
            "pixel_values": pixel_values.squeeze(0),
            "labels": labels.squeeze(0),
        }


def load_trocr_dataset(
    dataset_root: str | Path,
    processor: TrOCRProcessor,
    validation_split: float = 0.2,
    seed: int = 42,
    lowercase: bool = False,
    collapse_whitespace: bool = False,
    max_samples: Optional[int] = None,
    max_text_length: int | None = None,
) -> tuple[TrOCRDataset, TrOCRDataset]:
    """
    Load dataset for TrOCR fine-tuning.
    Returns (train_dataset, val_dataset).
    """
    pairs = discover_ocr_pairs(
        dataset_root,
        lowercase=lowercase,
        collapse_whitespace=collapse_whitespace,
        max_samples=max_samples,
    )

    pairs = _filter_pairs_by_length(pairs, max_text_length)

    if len(pairs) < 2:
        raise ValueError("At least two paired samples are required to build a train/validation split.")

    # Shuffle with seed for reproducibility
    rng = np.random.default_rng(seed)
    indices = rng.permutation(len(pairs))
    pairs = [pairs[i] for i in indices]

    # Split train/val
    validation_count = max(1, int(len(pairs) * validation_split))
    if validation_count >= len(pairs):
        validation_count = len(pairs) - 1

    split_index = len(pairs) - validation_count
    train_pairs = pairs[:split_index]
    val_pairs = pairs[split_index:]

    train_images = [str(p.image_path) for p in train_pairs]
    train_texts = [p.transcription for p in train_pairs]
    val_images = [str(p.image_path) for p in val_pairs]
    val_texts = [p.transcription for p in val_pairs]

    train_dataset = TrOCRDataset(
        image_paths=train_images,
        transcriptions=train_texts,
        processor=processor,
        augment=True,  # Use augmentation during training
    )

    val_dataset = TrOCRDataset(
        image_paths=val_images,
        transcriptions=val_texts,
        processor=processor,
        augment=False,  # No augmentation on validation
    )

    return train_dataset, val_dataset


def create_trocr_dataloaders(
    dataset_root: str | Path,
    processor: TrOCRProcessor,
    batch_size: int = 8,
    num_workers: int = 0,
    validation_split: float = 0.2,
    seed: int = 42,
    lowercase: bool = False,
    collapse_whitespace: bool = False,
    max_samples: Optional[int] = None,
    max_text_length: int | None = None,
) -> tuple[DataLoader, DataLoader]:
    """
    Create PyTorch DataLoaders for TrOCR training and validation.
    """
    train_dataset, val_dataset = load_trocr_dataset(
        dataset_root=dataset_root,
        processor=processor,
        validation_split=validation_split,
        seed=seed,
        lowercase=lowercase,
        collapse_whitespace=collapse_whitespace,
        max_samples=max_samples,
        max_text_length=max_text_length,
    )

    def collate_fn(batch):
        """Custom collate function to stack tensors."""
        pixel_values = torch.stack([item["pixel_values"] for item in batch])
        labels = torch.stack([item["labels"] for item in batch])
        return {
            "pixel_values": pixel_values,
            "labels": labels,
        }

    train_loader = DataLoader(
        train_dataset,
        batch_size=batch_size,
        shuffle=True,
        num_workers=num_workers,
        collate_fn=collate_fn,
    )

    val_loader = DataLoader(
        val_dataset,
        batch_size=batch_size,
        shuffle=False,
        num_workers=num_workers,
        collate_fn=collate_fn,
    )

    return train_loader, val_loader
