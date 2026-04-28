from __future__ import annotations

import argparse
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Convert a Keras model to TFLite.")
    parser.add_argument("--model", required=True, help="Path to a .keras model or SavedModel directory.")
    parser.add_argument("--output", required=True, help="Destination .tflite file.")
    parser.add_argument(
        "--optimization",
        choices=("none", "dynamic", "float16"),
        default="none",
        help="TFLite optimization mode. Use dynamic for default post-training optimization.",
    )
    parser.add_argument(
        "--quantize",
        action="store_true",
        help="Compatibility alias for --optimization dynamic.",
    )
    return parser.parse_args()


def path_size(path: Path) -> int:
    if path.is_file():
        return path.stat().st_size
    return sum(child.stat().st_size for child in path.rglob("*") if child.is_file())


def format_size(byte_count: int) -> str:
    value = float(byte_count)
    for unit in ("B", "KB", "MB", "GB"):
        if value < 1024 or unit == "GB":
            return f"{value:.1f} {unit}"
        value /= 1024
    return f"{byte_count} B"


def main() -> None:
    import tensorflow as tf

    args = parse_args()
    model_path = Path(args.model)
    output_path = Path(args.output)
    optimization = "dynamic" if args.quantize else args.optimization

    if not model_path.exists():
        raise FileNotFoundError(f"Model path does not exist: {model_path}")
    if output_path.suffix.lower() != ".tflite":
        raise ValueError(f"Output must be a .tflite file: {output_path}")

    output_path.parent.mkdir(parents=True, exist_ok=True)

    keras_model = tf.keras.models.load_model(model_path, compile=False)
    converter = tf.lite.TFLiteConverter.from_keras_model(keras_model)
    if optimization in {"dynamic", "float16"}:
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
    if optimization == "float16":
        converter.target_spec.supported_types = [tf.float16]

    tflite_model = converter.convert()
    output_path.write_bytes(tflite_model)
    input_size = path_size(model_path)
    output_size = output_path.stat().st_size
    ratio = output_size / input_size if input_size else 0

    print(f"Saved TFLite model to: {output_path}")
    print(f"Optimization: {optimization}")
    print(f"Input size: {format_size(input_size)}")
    print(f"Output size: {format_size(output_size)}")
    print(f"Output/Input ratio: {ratio:.2%}")


if __name__ == "__main__":
    main()
