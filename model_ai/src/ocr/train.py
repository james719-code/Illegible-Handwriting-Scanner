from __future__ import annotations

import json
from dataclasses import asdict, dataclass
from pathlib import Path

from .data import load_ocr_dataset
from .model import build_ocr_model
from .text import decode_ctc_prediction_batch, save_vocabulary
from training_runtime import configure_tensorflow_runtime


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
    device: str = "auto"
    mixed_precision: bool = True


class OCRTrainer:
    def __init__(self, config: OCRTrainConfig) -> None:
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
        training_model = self._build_ctc_training_model(
            tf=tf,
            prediction_model=model,
            max_text_length=dataset.max_text_length,
            learning_rate=self.config.learning_rate,
        )
        time_steps = int(model.output_shape[1])
        if dataset.max_text_length > time_steps:
            raise ValueError(
                "CRNN-CTC output has fewer timesteps than the longest label "
                f"({time_steps} < {dataset.max_text_length}). Increase --image-width "
                "or filter long samples with --max-text-length."
            )

        output_root = Path(self.config.output_root)
        checkpoint_dir = output_root / "checkpoints" / "ocr"
        exported_dir = output_root / "exported" / "ocr"
        checkpoint_dir.mkdir(parents=True, exist_ok=True)
        exported_dir.mkdir(parents=True, exist_ok=True)

        callbacks = [
            self._build_save_best_prediction_callback(
                tf=tf,
                prediction_model=model,
                filepath=checkpoint_dir / "best_model.keras",
                monitor="val_loss",
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

        train_dataset = self._make_ctc_dataset(
            tf=tf,
            images=dataset.train_images,
            labels=dataset.train_labels,
            texts=dataset.train_texts,
            input_time_steps=time_steps,
            batch_size=self.config.batch_size,
            shuffle=True,
            seed=self.config.seed,
        )
        val_dataset = self._make_ctc_dataset(
            tf=tf,
            images=dataset.val_images,
            labels=dataset.val_labels,
            texts=dataset.val_texts,
            input_time_steps=time_steps,
            batch_size=self.config.batch_size,
            shuffle=False,
            seed=self.config.seed,
        )

        history = training_model.fit(
            train_dataset,
            validation_data=val_dataset,
            epochs=self.config.epochs,
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
        persisted_config["algorithm"] = "crnn_ctc"
        persisted_config["output_time_steps"] = time_steps
        persisted_config["runtime"] = asdict(runtime)
        config_path.write_text(json.dumps(persisted_config, indent=2), encoding="utf-8")
        save_vocabulary(
            vocabulary_path,
            dataset.vocabulary,
            image_width=self.config.image_width,
            image_height=self.config.image_height,
            max_text_length=dataset.max_text_length,
            decoder="ctc",
            blank_index=len(dataset.vocabulary),
        )

        preview_count = min(5, len(dataset.val_images))
        preview_predictions = model.predict(dataset.val_images[:preview_count], verbose=0)
        preview_text = decode_ctc_prediction_batch(
            preview_predictions,
            dataset.vocabulary,
            blank_index=len(dataset.vocabulary),
        )
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

    def _build_ctc_training_model(
        self,
        *,
        tf,
        prediction_model,
        max_text_length: int,
        learning_rate: float,
    ):
        labels = tf.keras.Input(shape=(max_text_length,), dtype="int32", name="labels")
        input_length = tf.keras.Input(shape=(1,), dtype="int32", name="input_length")
        label_length = tf.keras.Input(shape=(1,), dtype="int32", name="label_length")
        ctc_loss = tf.keras.layers.Lambda(
            lambda args: tf.keras.backend.ctc_batch_cost(args[0], args[1], args[2], args[3]),
            output_shape=(1,),
            name="ctc_loss",
        )([labels, prediction_model.output, input_length, label_length])

        training_model = tf.keras.Model(
            inputs=[prediction_model.input, labels, input_length, label_length],
            outputs=ctc_loss,
            name="crnn_ctc_ocr_trainer",
        )
        training_model.compile(
            optimizer=tf.keras.optimizers.Adam(learning_rate=learning_rate),
            loss=lambda y_true, y_pred: y_pred,
        )
        return training_model

    def _make_ctc_dataset(
        self,
        *,
        tf,
        images,
        labels,
        texts: list[str],
        input_time_steps: int,
        batch_size: int,
        shuffle: bool,
        seed: int,
    ):
        import numpy as np

        sample_count = len(images)
        inputs = {
            "image": images,
            "labels": labels.astype("int32"),
            "input_length": np.full((sample_count, 1), input_time_steps, dtype=np.int32),
            "label_length": np.asarray(
                [[min(len(text), labels.shape[1])] for text in texts],
                dtype=np.int32,
            ),
        }
        targets = np.zeros((sample_count, 1), dtype=np.float32)
        dataset = tf.data.Dataset.from_tensor_slices((inputs, targets))
        if shuffle:
            dataset = dataset.shuffle(
                buffer_size=min(sample_count, 10_000),
                seed=seed,
                reshuffle_each_iteration=True,
            )
        return dataset.batch(batch_size).prefetch(tf.data.AUTOTUNE)

    def _build_save_best_prediction_callback(
        self,
        *,
        tf,
        prediction_model,
        filepath: Path,
        monitor: str,
    ):
        class SaveBestPredictionModel(tf.keras.callbacks.Callback):
            def __init__(self) -> None:
                super().__init__()
                self.filepath = Path(filepath)
                self.best = float("inf")

            def on_train_begin(self, logs=None) -> None:
                self.filepath.parent.mkdir(parents=True, exist_ok=True)

            def on_epoch_end(self, epoch: int, logs=None) -> None:
                logs = logs or {}
                current = logs.get(monitor)
                if current is None or current >= self.best:
                    return
                self.best = float(current)
                prediction_model.save(self.filepath)
                print(f"Saved best CRNN-CTC inference model to {self.filepath}")

        return SaveBestPredictionModel()
