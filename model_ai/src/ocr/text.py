from __future__ import annotations

import json
from pathlib import Path

import numpy as np


PAD_TOKEN = "<PAD>"
UNKNOWN_TOKEN = "<UNK>"


def normalize_transcription(
    text: str,
    *,
    lowercase: bool = False,
    collapse_whitespace: bool = False,
) -> str:
    normalized = text.replace("\r\n", "\n").replace("\r", "\n").strip("\n")
    if collapse_whitespace:
        normalized = " ".join(normalized.split())
    if lowercase:
        normalized = normalized.lower()
    return normalized


def build_vocabulary(texts: list[str]) -> list[str]:
    charset = sorted({character for text in texts for character in text})
    if not charset:
        raise ValueError("Unable to build an OCR vocabulary from an empty transcription set.")
    return [PAD_TOKEN, UNKNOWN_TOKEN, *charset]


def encode_text(
    text: str,
    vocabulary: list[str],
    max_length: int,
) -> list[int]:
    token_to_index = {token: index for index, token in enumerate(vocabulary)}
    unknown_index = token_to_index[UNKNOWN_TOKEN]
    encoded = [token_to_index.get(character, unknown_index) for character in text[:max_length]]
    padding = [token_to_index[PAD_TOKEN]] * max(0, max_length - len(encoded))
    return encoded + padding


def decode_indices(indices: list[int] | np.ndarray, vocabulary: list[str]) -> str:
    pad_index = vocabulary.index(PAD_TOKEN)
    unknown_index = vocabulary.index(UNKNOWN_TOKEN)
    characters: list[str] = []
    for index in indices:
        if int(index) == pad_index:
            continue
        if int(index) == unknown_index:
            characters.append("?")
            continue
        characters.append(vocabulary[int(index)])
    return "".join(characters).rstrip()


def decode_prediction_batch(predictions: np.ndarray, vocabulary: list[str]) -> list[str]:
    token_ids = np.argmax(predictions, axis=-1)
    return [decode_indices(sample_ids, vocabulary) for sample_ids in token_ids]


def decode_ctc_prediction_batch(
    predictions: np.ndarray,
    vocabulary: list[str],
    *,
    blank_index: int | None = None,
) -> list[str]:
    blank_index = len(vocabulary) if blank_index is None else blank_index
    pad_index = vocabulary.index(PAD_TOKEN)
    unknown_index = vocabulary.index(UNKNOWN_TOKEN)
    token_ids = np.argmax(predictions, axis=-1)
    decoded_batch: list[str] = []

    for sample_ids in token_ids:
        previous = None
        characters: list[str] = []
        for raw_index in sample_ids:
            index = int(raw_index)
            if index == previous:
                continue
            previous = index
            if index in {blank_index, pad_index}:
                continue
            if index == unknown_index:
                characters.append("?")
                continue
            if 0 <= index < len(vocabulary):
                characters.append(vocabulary[index])
        decoded_batch.append("".join(characters).rstrip())

    return decoded_batch


def save_vocabulary(
    output_path: str | Path,
    vocabulary: list[str],
    *,
    image_width: int,
    image_height: int,
    max_text_length: int,
    decoder: str = "argmax",
    blank_index: int | None = None,
) -> Path:
    output_path = Path(output_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(
        json.dumps(
            {
                "tokens": vocabulary,
                "image_width": image_width,
                "image_height": image_height,
                "max_text_length": max_text_length,
                "decoder": decoder,
                "blank_index": len(vocabulary) if blank_index is None else blank_index,
            },
            indent=2,
            ensure_ascii=False,
        ),
        encoding="utf-8",
    )
    return output_path


def load_vocabulary(input_path: str | Path) -> dict[str, object]:
    payload = json.loads(Path(input_path).read_text(encoding="utf-8"))
    tokens = payload.get("tokens")
    if not tokens:
        raise ValueError(f"Vocabulary file is missing tokens: {input_path}")
    return payload
