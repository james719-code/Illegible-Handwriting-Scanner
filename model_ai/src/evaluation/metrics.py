from __future__ import annotations

import numpy as np


def classification_report(
    y_true: np.ndarray,
    y_pred: np.ndarray,
    threshold: float = 0.5,
) -> dict[str, object]:
    y_true = np.asarray(y_true).astype(int).reshape(-1)
    y_pred = np.asarray(y_pred).reshape(-1)
    y_hat = (y_pred >= threshold).astype(int)

    tp = int(np.sum((y_true == 1) & (y_hat == 1)))
    tn = int(np.sum((y_true == 0) & (y_hat == 0)))
    fp = int(np.sum((y_true == 0) & (y_hat == 1)))
    fn = int(np.sum((y_true == 1) & (y_hat == 0)))

    total = max(len(y_true), 1)
    precision = tp / max(tp + fp, 1)
    recall = tp / max(tp + fn, 1)
    f1 = 2 * precision * recall / max(precision + recall, 1e-8)
    accuracy = (tp + tn) / total

    return {
        "accuracy": accuracy,
        "precision": precision,
        "recall": recall,
        "f1": f1,
        "support": int(len(y_true)),
        "confusion_matrix": {
            "true_negative": tn,
            "false_positive": fp,
            "false_negative": fn,
            "true_positive": tp,
        },
    }


def dice_score(
    y_true: np.ndarray,
    y_pred: np.ndarray,
    threshold: float = 0.5,
    smooth: float = 1e-6,
) -> float:
    y_true = np.asarray(y_true).astype(np.float32).reshape(-1)
    y_hat = (np.asarray(y_pred).astype(np.float32).reshape(-1) >= threshold).astype(np.float32)
    intersection = float(np.sum(y_true * y_hat))
    denominator = float(np.sum(y_true) + np.sum(y_hat))
    return (2.0 * intersection + smooth) / (denominator + smooth)


def intersection_over_union(
    y_true: np.ndarray,
    y_pred: np.ndarray,
    threshold: float = 0.5,
    smooth: float = 1e-6,
) -> float:
    y_true = np.asarray(y_true).astype(np.float32).reshape(-1)
    y_hat = (np.asarray(y_pred).astype(np.float32).reshape(-1) >= threshold).astype(np.float32)
    intersection = float(np.sum(y_true * y_hat))
    union = float(np.sum(y_true) + np.sum(y_hat) - intersection)
    return (intersection + smooth) / (union + smooth)


def detection_report(
    y_true: np.ndarray,
    y_pred: np.ndarray,
    threshold: float = 0.5,
) -> dict[str, float]:
    return {
        "dice": dice_score(y_true, y_pred, threshold=threshold),
        "iou": intersection_over_union(y_true, y_pred, threshold=threshold),
    }


def _edit_distance(sequence_a: list[str], sequence_b: list[str]) -> int:
    rows = len(sequence_a) + 1
    cols = len(sequence_b) + 1
    matrix = [[0] * cols for _ in range(rows)]

    for row in range(rows):
        matrix[row][0] = row
    for col in range(cols):
        matrix[0][col] = col

    for row in range(1, rows):
        for col in range(1, cols):
            cost = 0 if sequence_a[row - 1] == sequence_b[col - 1] else 1
            matrix[row][col] = min(
                matrix[row - 1][col] + 1,
                matrix[row][col - 1] + 1,
                matrix[row - 1][col - 1] + cost,
            )

    return matrix[-1][-1]


def character_error_rate(references: list[str], predictions: list[str]) -> float:
    total_distance = 0
    total_characters = 0
    for reference, prediction in zip(references, predictions, strict=True):
        total_distance += _edit_distance(list(reference), list(prediction))
        total_characters += max(len(reference), 1)
    return total_distance / max(total_characters, 1)


def word_error_rate(references: list[str], predictions: list[str]) -> float:
    total_distance = 0
    total_words = 0
    for reference, prediction in zip(references, predictions, strict=True):
        reference_words = reference.split()
        prediction_words = prediction.split()
        total_distance += _edit_distance(reference_words, prediction_words)
        total_words += max(len(reference_words), 1)
    return total_distance / max(total_words, 1)


def ocr_report(references: list[str], predictions: list[str]) -> dict[str, object]:
    exact_matches = sum(1 for reference, prediction in zip(references, predictions, strict=True) if reference == prediction)
    sample_count = max(len(references), 1)
    return {
        "sample_count": len(references),
        "exact_match_rate": exact_matches / sample_count,
        "character_error_rate": character_error_rate(references, predictions),
        "word_error_rate": word_error_rate(references, predictions),
    }
