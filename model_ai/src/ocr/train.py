from __future__ import annotations

import json
from dataclasses import asdict, dataclass
from pathlib import Path

from .data import load_ocr_dataset
from .model import build_ocr_model
from .text import decode_prediction_batch, save_vocabulary


@dataclass(slots=True)
class OCRTrainConfig:
    dataset_dir: str
    output_root: str = "models"
    image_width: int = 256
    image_height: int = 64
    batch_size: int = 8
    epochs: int = 25
    validation_split: float = 0.2
    learning_rate: float = 1e-3
    seed: int = 42
    max_text_length: int | None = None
    max_samples: int | None = None
    lowercase: bool = False
    collapse_whitespace: bool = False


class OCRTrainer:
    def __init__(self, config: OCRTrainConfig) -> None:
        self.config = config

    def fit(self) -> dict[str, Path]:
        import tensorflow as tf

        image_size = (self.config.image_width, self.config.image_height)
        dataset = load_ocr_dataset(
            dataset_root=self.config.dataset_dir,
            image_size=image_size,
            validation_split=self.config.validation_split,
            seed=self.config.seed,
            max_text_length=self.config.max_text_length,
            lowercase=self.config.lowercase,
            collapse_whitespace=self.config.collapse_whitespace,
            max_samples=self.config.max_samples,
        )

        model = build_ocr_model(
            image_size=image_size,
            max_text_length=dataset.max_text_length,
            vocab_size=len(dataset.vocabulary),
            learning_rate=self.config.learning_rate,
        )

        output_root = Path(self.config.output_root)
        checkpoint_dir = output_root / "checkpoints" / "ocr"
        exported_dir = output_root / "exported" / "ocr"
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
                patience=5,
                restore_best_weights=True,
            ),
            tf.keras.callbacks.ReduceLROnPlateau(
                monitor="val_loss",
                factor=0.5,
                patience=2,
                min_lr=1e-6,
            ),
        ]

        history = model.fit(
            dataset.train_images,
            dataset.train_labels,
            validation_data=(dataset.val_images, dataset.val_labels),
            epochs=self.config.epochs,
            batch_size=self.config.batch_size,
            callbacks=callbacks,
            verbose=1,
        )

        final_model_path = exported_dir / "final_model.keras"
        history_path = exported_dir / "history.json"
        config_path = exported_dir / "train_config.json"
        vocabulary_path = exported_dir / "vocabulary.json"
        predictions_path = exported_dir / "sample_predictions.json"

        model.save(final_model_path)
        history_path.write_text(json.dumps(history.history, indent=2), encoding="utf-8")

        persisted_config = asdict(self.config)
        persisted_config["max_text_length"] = dataset.max_text_length
        config_path.write_text(json.dumps(persisted_config, indent=2), encoding="utf-8")
        save_vocabulary(
            vocabulary_path,
            dataset.vocabulary,
            image_width=self.config.image_width,
            image_height=self.config.image_height,
            max_text_length=dataset.max_text_length,
        )

        preview_count = min(5, len(dataset.val_images))
        preview_predictions = model.predict(dataset.val_images[:preview_count], verbose=0)
        preview_text = decode_prediction_batch(preview_predictions, dataset.vocabulary)
        predictions_payload = [
            {
                "image_path": dataset.val_paths[index],
                "target": dataset.val_texts[index],
                "prediction": preview_text[index],
            }
            for index in range(preview_count)
        ]
        predictions_path.write_text(json.dumps(predictions_payload, indent=2, ensure_ascii=False), encoding="utf-8")

        return {
            "model": final_model_path,
            "history": history_path,
            "config": config_path,
            "vocabulary": vocabulary_path,
            "samples": predictions_path,
            "best_checkpoint": checkpoint_dir / "best_model.keras",
        }
