from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import cv2
import numpy as np
import pandas as pd


@dataclass(slots=True)
class DetectionDataset:
    train_images: np.ndarray
    train_masks: np.ndarray
    val_images: np.ndarray
    val_masks: np.ndarray
    image_paths: list[str]


def _resolve_image_path(annotation_path: Path, raw_path: str) -> Path:
    candidate = Path(raw_path)
    if candidate.is_absolute():
        return candidate

    annotation_dir = annotation_path.parent
    if (annotation_dir / candidate).exists():
        return annotation_dir / candidate

    project_root = annotation_path.parents[2]
    data_root = project_root / "data"
    if (data_root / candidate).exists():
        return data_root / candidate

    return annotation_dir / candidate


def _load_image(path: Path, image_size: tuple[int, int]) -> tuple[np.ndarray, tuple[int, int]]:
    image = cv2.imread(str(path), cv2.IMREAD_GRAYSCALE)
    if image is None:
        raise FileNotFoundError(f"Unable to read image: {path}")

    original_height, original_width = image.shape[:2]
    resized = cv2.resize(image, image_size, interpolation=cv2.INTER_AREA)
    normalized = resized.astype(np.float32) / 255.0
    return normalized[..., np.newaxis], (original_width, original_height)


def _build_mask(
    boxes: pd.DataFrame,
    original_size: tuple[int, int],
    image_size: tuple[int, int],
) -> np.ndarray:
    original_width, original_height = original_size
    target_width, target_height = image_size
    mask = np.zeros((target_height, target_width), dtype=np.float32)

    x_scale = target_width / float(original_width)
    y_scale = target_height / float(original_height)

    for row in boxes.itertuples(index=False):
        x1 = int(np.clip(round(row.xmin * x_scale), 0, target_width - 1))
        y1 = int(np.clip(round(row.ymin * y_scale), 0, target_height - 1))
        x2 = int(np.clip(round(row.xmax * x_scale), 0, target_width - 1))
        y2 = int(np.clip(round(row.ymax * y_scale), 0, target_height - 1))

        if x2 <= x1 or y2 <= y1:
            continue
        cv2.rectangle(mask, (x1, y1), (x2, y2), color=1.0, thickness=-1)

    return mask[..., np.newaxis]


def load_detection_arrays(
    annotation_path: str | Path,
    image_size: tuple[int, int],
) -> tuple[np.ndarray, np.ndarray, list[str]]:
    annotation_path = Path(annotation_path)
    frame = pd.read_csv(annotation_path)
    if frame.empty:
        raise ValueError(f"Annotation CSV is empty: {annotation_path}")
    required = {"image_path", "xmin", "ymin", "xmax", "ymax"}
    if not required.issubset(frame.columns):
        missing = required.difference(frame.columns)
        raise ValueError(f"Annotation CSV is missing columns: {sorted(missing)}")

    images: list[np.ndarray] = []
    masks: list[np.ndarray] = []
    image_paths: list[str] = []

    for image_path, group in frame.groupby("image_path", sort=False):
        resolved = _resolve_image_path(annotation_path, str(image_path))
        image, original_size = _load_image(resolved, image_size)
        mask = _build_mask(group, original_size, image_size)
        images.append(image)
        masks.append(mask)
        image_paths.append(str(resolved))

    if not images:
        raise ValueError(f"No annotated images were loaded from: {annotation_path}")

    return np.stack(images), np.stack(masks), image_paths


def load_detection_dataset(
    annotation_path: str | Path,
    image_size: tuple[int, int],
    validation_split: float = 0.2,
    seed: int = 42,
) -> DetectionDataset:
    images, masks, image_paths = load_detection_arrays(annotation_path, image_size)
    if len(images) < 2:
        raise ValueError("At least two annotated pages are required to build a train/validation split.")

    rng = np.random.default_rng(seed)
    indices = rng.permutation(len(images))
    images = images[indices]
    masks = masks[indices]
    ordered_paths = [image_paths[index] for index in indices]

    validation_count = max(1, int(len(images) * validation_split))
    if validation_count >= len(images):
        validation_count = len(images) - 1

    split_index = len(images) - validation_count
    return DetectionDataset(
        train_images=images[:split_index],
        train_masks=masks[:split_index],
        val_images=images[split_index:],
        val_masks=masks[split_index:],
        image_paths=ordered_paths,
    )
