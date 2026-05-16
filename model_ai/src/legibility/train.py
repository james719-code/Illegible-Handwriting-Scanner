from __future__ import annotations

import json
from dataclasses import asdict, dataclass
from pathlib import Path

from .data import load_legibility_dataset
from .model import build_legibility_model, save_label_map
from training_runtime import configure_tensorflow_runtime, make_tf_dataset


@dataclass(slots=True)
class ClassifierTrainConfig:
    manifest_path: str
    output_root: str = "models"
    image_width: int = 224
    image_height: int = 224
    batch_size: int = 16
    epochs: int = 15
    validation_split: float = 0.2
    learning_rate: float = 1e-3
    seed: int = 42
    device: str = "auto"
    mixed_precision: bool = True


class LegibilityTrainer:
    def __init__(self, config: ClassifierTrainConfig) -> None:
        self.config = config

    def fit(self) -> dict[str, Path]:
        import tensorflow as tf

        runtime = configure_tensorflow_runtime(
            tf,
            requested_device=self.config.device,
            mixed_precision=self.config.mixed_precision,
        )
        print(
            "TensorFlow runtime: "
            f"device={runtime.device}, "
            f"mixed_precision={runtime.mixed_precision}, "
            f"gpus={runtime.gpu_names or 'none'}"
        )

        image_size = (self.config.image_width, self.config.image_height)
        dataset = load_legibility_dataset(
            manifest_path=self.config.manifest_path,
            image_size=image_size,
            validation_split=self.config.validation_split,
            seed=self.config.seed,
        )

        model = build_legibility_model(
            image_size=image_size,
            channels=1,
            learning_rate=self.config.learning_rate,
        )

        output_root = Path(self.config.output_root)
        checkpoint_dir = output_root / "checkpoints" / "legibility"
        exported_dir = output_root / "exported" / "legibility"
        checkpoint_dir.mkdir(parents=True, exist_ok=True)
        exported_dir.mkdir(parents=True, exist_ok=True)

        callbacks = [
            tf.keras.callbacks.ModelCheckpoint(
                filepath=str(checkpoint_dir / "best_model.keras"),
                monitor="val_loss",
                mode="min",
                save_best_only=True,
            ),
            tf.keras.callbacks.EarlyStopping(
                monitor="val_loss",
                patience=4,
                restore_best_weights=True,
            ),
            tf.keras.callbacks.ReduceLROnPlateau(
                monitor="val_loss",
                factor=0.5,
                patience=2,
                min_lr=1e-6,
            ),
        ]

        train_dataset = make_tf_dataset(
            dataset.train_images,
            dataset.train_labels,
            batch_size=self.config.batch_size,
            shuffle=True,
            seed=self.config.seed,
        )
        val_dataset = make_tf_dataset(
            dataset.val_images,
            dataset.val_labels,
            batch_size=self.config.batch_size,
            shuffle=False,
            seed=self.config.seed,
        )

        history = model.fit(
            train_dataset,
            validation_data=val_dataset,
            epochs=self.config.epochs,
            callbacks=callbacks,
            verbose=1,
        )

        final_model_path = exported_dir / "final_model.keras"
        history_path = exported_dir / "history.json"
        config_path = exported_dir / "train_config.json"
        labels_path = exported_dir / "labels.json"

        model.save(final_model_path)
        history_path.write_text(json.dumps(history.history, indent=2), encoding="utf-8")
        persisted_config = asdict(self.config)
        persisted_config["runtime"] = asdict(runtime)
        config_path.write_text(json.dumps(persisted_config, indent=2), encoding="utf-8")
        save_label_map(labels_path, dataset.label_names)

        return {
            "model": final_model_path,
            "history": history_path,
            "config": config_path,
            "labels": labels_path,
            "best_checkpoint": checkpoint_dir / "best_model.keras",
        }
