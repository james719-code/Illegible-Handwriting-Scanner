from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import cv2
import numpy as np
import pandas as pd


LABEL_LOOKUP = {
    "legible": 0,
    "illegible": 1,
    "0": 0,
    "1": 1,
    0: 0,
    1: 1,
}


@dataclass(slots=True)
class LegibilityDataset:
    train_images: np.ndarray
    train_labels: np.ndarray
    val_images: np.ndarray
    val_labels: np.ndarray
    label_names: list[str]


def _resolve_image_path(manifest_path: Path, raw_path: str) -> Path:
    candidate = Path(raw_path)
    if candidate.is_absolute():
        return candidate

    annotation_dir = manifest_path.parent
    if (annotation_dir / candidate).exists():
        return annotation_dir / candidate

    project_root = manifest_path.parents[2]
    data_root = project_root / "data"
    if (data_root / candidate).exists():
        return data_root / candidate

    return manifest_path.parent / candidate


def _normalize_label(value: object) -> int:
    lowered = str(value).strip().lower()
    if lowered not in LABEL_LOOKUP:
        raise ValueError(f"Unsupported label value: {value}")
    return LABEL_LOOKUP[lowered]


def _load_image(image_path: Path, image_size: tuple[int, int]) -> np.ndarray:
    image = cv2.imread(str(image_path), cv2.IMREAD_GRAYSCALE)
    if image is None:
        raise FileNotFoundError(f"Unable to read image: {image_path}")

    resized = cv2.resize(image, image_size, interpolation=cv2.INTER_AREA)
    normalized = resized.astype(np.float32) / 255.0
    return normalized[..., np.newaxis]


def load_legibility_arrays(
    manifest_path: str | Path,
    image_size: tuple[int, int],
) -> tuple[np.ndarray, np.ndarray, list[str]]:
    manifest_path = Path(manifest_path)
    frame = pd.read_csv(manifest_path)
    if frame.empty:
        raise ValueError(f"Manifest is empty: {manifest_path}")
    required = {"image_path", "label"}
    if not required.issubset(frame.columns):
        missing = required.difference(frame.columns)
        raise ValueError(f"Manifest is missing columns: {sorted(missing)}")

    images: list[np.ndarray] = []
    labels: list[int] = []
    for row in frame.itertuples(index=False):
        resolved = _resolve_image_path(manifest_path, str(row.image_path))
        images.append(_load_image(resolved, image_size))
        labels.append(_normalize_label(row.label))

    if not images:
        raise ValueError(f"No images were loaded from manifest: {manifest_path}")

    label_names = ["legible", "illegible"]
    return np.stack(images), np.asarray(labels, dtype=np.float32), label_names


def load_legibility_dataset(
    manifest_path: str | Path,
    image_size: tuple[int, int],
    validation_split: float = 0.2,
    seed: int = 42,
) -> LegibilityDataset:
    images, labels, label_names = load_legibility_arrays(manifest_path, image_size)
    if len(images) < 2:
        raise ValueError("At least two samples are required to build a train/validation split.")

    rng = np.random.default_rng(seed)
    indices = rng.permutation(len(images))
    images = images[indices]
    labels = labels[indices]

    validation_count = max(1, int(len(images) * validation_split))
    if validation_count >= len(images):
        validation_count = len(images) - 1

    split_index = len(images) - validation_count
    return LegibilityDataset(
        train_images=images[:split_index],
        train_labels=labels[:split_index],
        val_images=images[split_index:],
        val_labels=labels[split_index:],
        label_names=label_names,
    )
