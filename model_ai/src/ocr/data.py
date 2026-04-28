from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import cv2
import numpy as np

from .text import build_vocabulary, encode_text, normalize_transcription


IMAGE_SUFFIXES = {".png", ".jpg", ".jpeg", ".bmp", ".tif", ".tiff"}


@dataclass(slots=True)
class OCRPair:
    image_path: Path
    transcript_path: Path
    transcription: str


@dataclass(slots=True)
class OCRDataset:
    train_images: np.ndarray
    train_labels: np.ndarray
    val_images: np.ndarray
    val_labels: np.ndarray
    train_texts: list[str]
    val_texts: list[str]
    train_paths: list[str]
    val_paths: list[str]
    vocabulary: list[str]
    max_text_length: int


@dataclass(slots=True)
class OCRPredictionBatch:
    images: np.ndarray
    texts: list[str]
    image_paths: list[str]


def inspect_dataset_root(
    dataset_root: str | Path,
    *,
    lowercase: bool = False,
    collapse_whitespace: bool = False,
    max_samples: int | None = None,
) -> dict[str, object]:
    dataset_root = Path(dataset_root)
    image_files = sorted(
        path
        for path in dataset_root.rglob("*")
        if path.is_file() and path.suffix.lower() in IMAGE_SUFFIXES
    )
    paired: list[OCRPair] = []
    missing_transcripts: list[str] = []

    for image_path in image_files:
        transcript_path = image_path.with_suffix(".txt")
        if not transcript_path.exists():
            missing_transcripts.append(str(image_path))
            continue

        transcription = normalize_transcription(
            transcript_path.read_text(encoding="utf-8"),
            lowercase=lowercase,
            collapse_whitespace=collapse_whitespace,
        )
        if not transcription.strip():
            raise ValueError(f"Transcript file is empty: {transcript_path}")

        paired.append(
            OCRPair(
                image_path=image_path,
                transcript_path=transcript_path,
                transcription=transcription,
            ),
        )
        if max_samples is not None and len(paired) >= max_samples:
            break

    max_text_length = max((len(sample.transcription) for sample in paired), default=0)
    average_text_length = (
        float(sum(len(sample.transcription) for sample in paired)) / len(paired)
        if paired
        else 0.0
    )
    charset = sorted({character for sample in paired for character in sample.transcription})

    return {
        "pairs": paired,
        "missing_transcripts": missing_transcripts,
        "pair_count": len(paired),
        "missing_count": len(missing_transcripts),
        "max_text_length": max_text_length,
        "average_text_length": average_text_length,
        "charset": charset,
    }


def discover_ocr_pairs(
    dataset_root: str | Path,
    *,
    lowercase: bool = False,
    collapse_whitespace: bool = False,
    max_samples: int | None = None,
    max_transcription_length: int | None = None,
) -> list[OCRPair]:
    summary = inspect_dataset_root(
        dataset_root,
        lowercase=lowercase,
        collapse_whitespace=collapse_whitespace,
        max_samples=max_samples,
    )
    pairs = list(summary["pairs"])
    if max_transcription_length is not None:
        pairs = [pair for pair in pairs if len(pair.transcription) <= max_transcription_length]
    if not pairs:
        raise ValueError(f"No image + transcript pairs were found in: {dataset_root}")
    return pairs


def load_ocr_image(
    image_path: str | Path,
    image_size: tuple[int, int],
) -> np.ndarray:
    image = cv2.imread(str(image_path), cv2.IMREAD_GRAYSCALE)
    if image is None:
        raise FileNotFoundError(f"Unable to read image: {image_path}")

    target_width, target_height = image_size
    height, width = image.shape[:2]
    scale = min(target_width / max(width, 1), target_height / max(height, 1))
    scaled_width = max(1, int(width * scale))
    scaled_height = max(1, int(height * scale))
    resized = cv2.resize(image, (scaled_width, scaled_height), interpolation=cv2.INTER_AREA)

    canvas = np.full((target_height, target_width), 255, dtype=np.uint8)
    x_offset = (target_width - scaled_width) // 2
    y_offset = (target_height - scaled_height) // 2
    canvas[y_offset:y_offset + scaled_height, x_offset:x_offset + scaled_width] = resized
    normalized = canvas.astype(np.float32) / 255.0
    return normalized[..., np.newaxis]


def load_ocr_arrays(
    dataset_root: str | Path,
    image_size: tuple[int, int],
    vocabulary: list[str] | None = None,
    max_text_length: int | None = None,
    *,
    lowercase: bool = False,
    collapse_whitespace: bool = False,
    max_samples: int | None = None,
) -> tuple[np.ndarray, np.ndarray, list[str], list[str], list[str], int]:
    pairs = discover_ocr_pairs(
        dataset_root,
        lowercase=lowercase,
        collapse_whitespace=collapse_whitespace,
        max_samples=max_samples,
    )
    texts = [pair.transcription for pair in pairs]
    vocabulary = vocabulary or build_vocabulary(texts)
    max_text_length = max_text_length or max(len(text) for text in texts)

    images = [load_ocr_image(pair.image_path, image_size) for pair in pairs]
    labels = [encode_text(pair.transcription, vocabulary, max_text_length) for pair in pairs]
    paths = [str(pair.image_path) for pair in pairs]

    return (
        np.stack(images),
        np.asarray(labels, dtype=np.int32),
        texts,
        paths,
        vocabulary,
        max_text_length,
    )


def load_ocr_prediction_batch(
    dataset_root: str | Path,
    image_size: tuple[int, int],
    *,
    lowercase: bool = False,
    collapse_whitespace: bool = False,
    max_samples: int | None = None,
    max_transcription_length: int | None = None,
) -> OCRPredictionBatch:
    pairs = discover_ocr_pairs(
        dataset_root,
        lowercase=lowercase,
        collapse_whitespace=collapse_whitespace,
        max_samples=max_samples,
        max_transcription_length=max_transcription_length,
    )
    return OCRPredictionBatch(
        images=np.stack([load_ocr_image(pair.image_path, image_size) for pair in pairs]),
        texts=[pair.transcription for pair in pairs],
        image_paths=[str(pair.image_path) for pair in pairs],
    )


def load_ocr_dataset(
    dataset_root: str | Path,
    image_size: tuple[int, int],
    validation_split: float = 0.2,
    seed: int = 42,
    vocabulary: list[str] | None = None,
    max_text_length: int | None = None,
    *,
    lowercase: bool = False,
    collapse_whitespace: bool = False,
    max_samples: int | None = None,
) -> OCRDataset:
    images, labels, texts, paths, vocabulary, max_text_length = load_ocr_arrays(
        dataset_root=dataset_root,
        image_size=image_size,
        vocabulary=vocabulary,
        max_text_length=max_text_length,
        lowercase=lowercase,
        collapse_whitespace=collapse_whitespace,
        max_samples=max_samples,
    )
    if len(images) < 2:
        raise ValueError("At least two paired samples are required to build a train/validation split.")

    rng = np.random.default_rng(seed)
    indices = rng.permutation(len(images))
    images = images[indices]
    labels = labels[indices]
    ordered_texts = [texts[index] for index in indices]
    ordered_paths = [paths[index] for index in indices]

    validation_count = max(1, int(len(images) * validation_split))
    if validation_count >= len(images):
        validation_count = len(images) - 1

    split_index = len(images) - validation_count
    return OCRDataset(
        train_images=images[:split_index],
        train_labels=labels[:split_index],
        val_images=images[split_index:],
        val_labels=labels[split_index:],
        train_texts=ordered_texts[:split_index],
        val_texts=ordered_texts[split_index:],
        train_paths=ordered_paths[:split_index],
        val_paths=ordered_paths[split_index:],
        vocabulary=vocabulary,
        max_text_length=max_text_length,
    )
