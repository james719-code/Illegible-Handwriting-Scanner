from __future__ import annotations

from pathlib import Path

import cv2
import numpy as np

from .data import load_ocr_image
from .text import decode_ctc_prediction_batch
from .text import decode_prediction_batch as decode_sequences
from .text import load_vocabulary


def decode_prediction_batch(
    predictions: np.ndarray,
    vocabulary: list[str],
    *,
    decoder: str = "argmax",
    blank_index: int | None = None,
) -> list[str]:
    if decoder == "ctc":
        return decode_ctc_prediction_batch(predictions, vocabulary, blank_index=blank_index)
    return decode_sequences(predictions, vocabulary)


def predict_batch(
    model_path: str | Path,
    vocabulary_path: str | Path,
    image_paths: list[str | Path],
    image_size: tuple[int, int],
) -> dict[str, str]:
    import tensorflow as tf

    vocabulary = load_vocabulary(vocabulary_path)
    images = np.stack([load_ocr_image(path, image_size) for path in image_paths])
    model = tf.keras.models.load_model(model_path, compile=False)
    predictions = model.predict(images, verbose=0)
    decoded = decode_prediction_batch(
        predictions,
        list(vocabulary["tokens"]),
        decoder=str(vocabulary.get("decoder", "argmax")),
        blank_index=int(vocabulary.get("blank_index", len(vocabulary["tokens"]))),
    )
    return {str(path): decoded[index] for index, path in enumerate(image_paths)}


def predict_text(
    model_path: str | Path,
    vocabulary_path: str | Path,
    image_path: str | Path,
    image_size: tuple[int, int],
) -> str:
    return predict_batch(
        model_path=model_path,
        vocabulary_path=vocabulary_path,
        image_paths=[image_path],
        image_size=image_size,
    )[str(image_path)]


def run_tesseract(
    image_path: str | Path,
    lang: str = "eng",
    psm: int = 6,
) -> str:
    import pytesseract

    image = cv2.imread(str(image_path))
    if image is None:
        raise FileNotFoundError(f"Unable to read image: {image_path}")

    config = f"--oem 1 --psm {psm}"
    return pytesseract.image_to_string(image, lang=lang, config=config).strip()


def batch_ocr(
    image_paths: list[str | Path],
    lang: str = "eng",
    psm: int = 6,
) -> dict[str, str]:
    return {
        str(path): run_tesseract(path, lang=lang, psm=psm)
        for path in image_paths
    }
