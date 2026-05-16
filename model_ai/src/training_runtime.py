from __future__ import annotations

from dataclasses import dataclass
from typing import Any

import numpy as np


@dataclass(slots=True)
class TensorFlowRuntime:
    device: str
    gpu_names: list[str]
    mixed_precision: bool


def configure_tensorflow_runtime(
    tf: Any,
    *,
    requested_device: str = "auto",
    mixed_precision: bool = True,
) -> TensorFlowRuntime:
    """Configure TensorFlow once before model creation."""
    normalized_device = requested_device.lower()
    if normalized_device not in {"auto", "gpu", "cpu"}:
        raise ValueError("requested_device must be one of: auto, gpu, cpu")

    gpus = tf.config.list_physical_devices("GPU")
    if normalized_device == "cpu":
        if gpus:
            tf.config.set_visible_devices([], "GPU")
        gpus = []
    else:
        for gpu in gpus:
            try:
                tf.config.experimental.set_memory_growth(gpu, True)
            except RuntimeError:
                # Device was already initialized; continue with the current runtime.
                pass

    enabled_mixed_precision = bool(mixed_precision and gpus)
    if enabled_mixed_precision:
        tf.keras.mixed_precision.set_global_policy("mixed_float16")
    else:
        tf.keras.mixed_precision.set_global_policy("float32")

    return TensorFlowRuntime(
        device="gpu" if gpus else "cpu",
        gpu_names=[gpu.name for gpu in gpus],
        mixed_precision=enabled_mixed_precision,
    )


def make_tf_dataset(
    features: np.ndarray,
    targets: np.ndarray,
    *,
    batch_size: int,
    shuffle: bool,
    seed: int,
):
    import tensorflow as tf

    dataset = tf.data.Dataset.from_tensor_slices((features, targets))
    if shuffle:
        dataset = dataset.shuffle(
            buffer_size=min(len(features), 10_000),
            seed=seed,
            reshuffle_each_iteration=True,
        )
    return dataset.batch(batch_size).prefetch(tf.data.AUTOTUNE)
