from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

import cv2
import numpy as np


@dataclass(slots=True)
class PreprocessConfig:
    target_width: int = 512
    target_height: int = 512
    clahe_clip_limit: float = 2.0
    clahe_tile_grid_size: int = 8
    median_blur_kernel: int = 3
    adaptive_block_size: int = 31
    adaptive_c: int = 15
    apply_binarization: bool = True


def _load_grayscale_image(image: np.ndarray | str | Path) -> np.ndarray:
    if isinstance(image, np.ndarray):
        if image.ndim == 2:
            return image
        return cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    loaded = cv2.imread(str(image), cv2.IMREAD_GRAYSCALE)
    if loaded is None:
        raise FileNotFoundError(f"Unable to read image: {image}")
    return loaded


def preprocess_image(
    image: np.ndarray | str | Path,
    config: PreprocessConfig | None = None,
) -> np.ndarray:
    config = config or PreprocessConfig()
    gray = _load_grayscale_image(image)
    resized = cv2.resize(
        gray,
        (config.target_width, config.target_height),
        interpolation=cv2.INTER_AREA,
    )

    clahe = cv2.createCLAHE(
        clipLimit=config.clahe_clip_limit,
        tileGridSize=(config.clahe_tile_grid_size, config.clahe_tile_grid_size),
    )
    enhanced = clahe.apply(resized)
    denoised = cv2.medianBlur(enhanced, config.median_blur_kernel)

    if not config.apply_binarization:
        return denoised

    return cv2.adaptiveThreshold(
        denoised,
        255,
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
        cv2.THRESH_BINARY,
        config.adaptive_block_size,
        config.adaptive_c,
    )


def preprocess_directory(
    input_dir: str | Path,
    output_dir: str | Path,
    config: PreprocessConfig | None = None,
    patterns: Iterable[str] = ("*.png", "*.jpg", "*.jpeg", "*.bmp", "*.tif", "*.tiff"),
) -> list[Path]:
    config = config or PreprocessConfig()
    input_dir = Path(input_dir)
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    saved_files: list[Path] = []
    seen: set[Path] = set()
    for pattern in patterns:
        for image_path in input_dir.rglob(pattern):
            if image_path in seen:
                continue
            seen.add(image_path)
            processed = preprocess_image(image_path, config)
            destination = output_dir / image_path.name
            cv2.imwrite(str(destination), processed)
            saved_files.append(destination)

    return saved_files
