from __future__ import annotations

import importlib.util


def _has_module(name: str) -> bool:
    return importlib.util.find_spec(name) is not None


def main() -> None:
    if _has_module("torch"):
        import torch

        print("PyTorch")
        print(f"  version: {torch.__version__}")
        print(f"  cuda available: {torch.cuda.is_available()}")
        if torch.cuda.is_available():
            print(f"  cuda version: {torch.version.cuda}")
            print(f"  gpu: {torch.cuda.get_device_name(0)}")
    else:
        print("PyTorch: not installed")

    if _has_module("tensorflow"):
        import tensorflow as tf

        gpus = tf.config.list_physical_devices("GPU")
        print("TensorFlow")
        print(f"  version: {tf.__version__}")
        print(f"  gpu count: {len(gpus)}")
        for gpu in gpus:
            print(f"  gpu: {gpu.name}")
    else:
        print("TensorFlow: not installed")


if __name__ == "__main__":
    main()
