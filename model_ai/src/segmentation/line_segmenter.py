from __future__ import annotations

from dataclasses import dataclass

import cv2
import numpy as np


@dataclass(slots=True)
class Segment:
    x: int
    y: int
    width: int
    height: int


class ProjectionProfileSegmenter:
    def __init__(self, min_run_length: int = 6) -> None:
        self.min_run_length = min_run_length

    def _binarize(self, image: np.ndarray) -> np.ndarray:
        if image.ndim == 3:
            image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

        _, thresholded = cv2.threshold(
            image,
            0,
            255,
            cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU,
        )
        return thresholded

    def _projection_runs(self, values: np.ndarray) -> list[tuple[int, int]]:
        runs: list[tuple[int, int]] = []
        start = None
        for index, value in enumerate(values):
            if value > 0 and start is None:
                start = index
            if value == 0 and start is not None:
                if index - start >= self.min_run_length:
                    runs.append((start, index))
                start = None
        if start is not None and len(values) - start >= self.min_run_length:
            runs.append((start, len(values)))
        return runs

    def segment_lines(self, image: np.ndarray) -> list[Segment]:
        binary = self._binarize(image)
        horizontal_projection = np.sum(binary > 0, axis=1)
        line_runs = self._projection_runs(horizontal_projection)
        height, width = binary.shape
        return [Segment(x=0, y=start, width=width, height=end - start) for start, end in line_runs]

    def segment_words(self, image: np.ndarray) -> list[Segment]:
        binary = self._binarize(image)
        vertical_projection = np.sum(binary > 0, axis=0)
        word_runs = self._projection_runs(vertical_projection)
        height, width = binary.shape
        return [Segment(x=start, y=0, width=end - start, height=height) for start, end in word_runs]
