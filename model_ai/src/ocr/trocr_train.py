from __future__ import annotations

import json
import os
import warnings
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Optional

import torch
from torch.optim import AdamW
from torch.optim.lr_scheduler import ReduceLROnPlateau

# Keep TrOCR pipeline on PyTorch only to avoid TensorFlow import side effects.
os.environ.setdefault("TRANSFORMERS_NO_TF", "1")
os.environ.setdefault("TRANSFORMERS_NO_FLAX", "1")
os.environ.setdefault("USE_TF", "0")
os.environ.setdefault("USE_FLAX", "0")

from transformers import TrOCRProcessor, VisionEncoderDecoderModel

from .trocr_data import create_trocr_dataloaders


def _get_available_device(requested_device: str = "cuda") -> str:
    """Detect available device, falling back to CPU if needed."""
    if requested_device == "cpu":
        return "cpu"
    
    if not torch.cuda.is_available():
        warnings.warn("CUDA not available, falling back to CPU", UserWarning)
        return "cpu"
    
    try:
        # Try to allocate a small tensor to verify CUDA actually works
        torch.empty(1, device="cuda")
        return "cuda"
    except (AssertionError, RuntimeError) as e:
        warnings.warn(
            f"CUDA not functional ({e}), falling back to CPU. "
            "Install CUDA-enabled PyTorch: pip install torch torchvision --index-url https://download.pytorch.org/whl/cu118",
            UserWarning,
        )
        return "cpu"


@dataclass(slots=True)
class TrOCRTrainConfig:
    dataset_dir: str
    output_root: str = "models"
    batch_size: int = 8
    epochs: int = 25
    validation_split: float = 0.2
    learning_rate: float = 1e-5
    seed: int = 42
    lowercase: bool = False
    collapse_whitespace: bool = False
    max_samples: Optional[int] = None
    max_text_length: Optional[int] = None
    model_name: str = "microsoft/trocr-base-handwritten"
    device: str = "cuda"
    freeze_encoder: bool = False
    num_workers: int = 0


class TrOCRTrainer:
    def __init__(self, config: TrOCRTrainConfig) -> None:
        self.config = config
        # Auto-detect best available device
        available_device = _get_available_device(config.device)
        self.device = torch.device(available_device)
        if available_device != config.device:
            self.config.device = available_device

    def fit(self) -> dict[str, Path]:
        """Fine-tune TrOCR model and save artifacts."""
        # Load processor and model
        processor = TrOCRProcessor.from_pretrained(self.config.model_name)
        model = VisionEncoderDecoderModel.from_pretrained(self.config.model_name)

        # Ensure decoder special token ids are configured for teacher-forced training.
        if model.config.decoder_start_token_id is None:
            model.config.decoder_start_token_id = processor.tokenizer.cls_token_id
        if model.config.pad_token_id is None:
            model.config.pad_token_id = processor.tokenizer.pad_token_id
        if model.config.eos_token_id is None and processor.tokenizer.sep_token_id is not None:
            model.config.eos_token_id = processor.tokenizer.sep_token_id

        model = model.to(self.device)

        # Optionally freeze encoder
        if self.config.freeze_encoder:
            for param in model.encoder.parameters():
                param.requires_grad = False

        # Create data loaders
        train_loader, val_loader = create_trocr_dataloaders(
            dataset_root=self.config.dataset_dir,
            processor=processor,
            batch_size=self.config.batch_size,
            validation_split=self.config.validation_split,
            seed=self.config.seed,
            lowercase=self.config.lowercase,
            collapse_whitespace=self.config.collapse_whitespace,
            max_samples=self.config.max_samples,
            max_text_length=self.config.max_text_length,
            num_workers=self.config.num_workers,
        )

        # Setup optimizer and scheduler
        optimizer = AdamW(model.parameters(), lr=self.config.learning_rate)
        scheduler = ReduceLROnPlateau(
            optimizer,
            mode="min",
            factor=0.5,
            patience=2,
            min_lr=1e-6,
        )

        # Create output directories
        output_root = Path(self.config.output_root)
        checkpoint_dir = output_root / "checkpoints" / "trocr"
        exported_dir = output_root / "exported" / "trocr"
        checkpoint_dir.mkdir(parents=True, exist_ok=True)
        exported_dir.mkdir(parents=True, exist_ok=True)

        # Training loop
        best_val_loss = float("inf")
        patience_counter = 0
        patience = 5
        history = {"train_loss": [], "val_loss": []}

        for epoch in range(self.config.epochs):
            # Training phase
            model.train()
            train_loss = 0.0
            for batch_idx, batch in enumerate(train_loader):
                pixel_values = batch["pixel_values"].to(self.device)
                labels = batch["labels"].to(self.device)

                # Forward pass
                outputs = model(pixel_values=pixel_values, labels=labels)
                loss = outputs.loss

                # Backward pass
                optimizer.zero_grad()
                loss.backward()
                optimizer.step()

                train_loss += loss.item()
                if batch_idx % 10 == 0:
                    print(
                        f"Epoch {epoch + 1}/{self.config.epochs}, "
                        f"Batch {batch_idx}/{len(train_loader)}, "
                        f"Loss: {loss.item():.4f}"
                    )

            avg_train_loss = train_loss / len(train_loader)
            history["train_loss"].append(avg_train_loss)
            print(f"Epoch {epoch + 1} - Avg Train Loss: {avg_train_loss:.4f}")

            # Validation phase
            model.eval()
            val_loss = 0.0
            with torch.no_grad():
                for batch in val_loader:
                    pixel_values = batch["pixel_values"].to(self.device)
                    labels = batch["labels"].to(self.device)

                    outputs = model(pixel_values=pixel_values, labels=labels)
                    loss = outputs.loss
                    val_loss += loss.item()

            avg_val_loss = val_loss / len(val_loader)
            history["val_loss"].append(avg_val_loss)
            print(f"Epoch {epoch + 1} - Avg Val Loss: {avg_val_loss:.4f}")

            # Learning rate scheduling
            scheduler.step(avg_val_loss)

            # Checkpoint best model
            if avg_val_loss < best_val_loss:
                best_val_loss = avg_val_loss
                patience_counter = 0
                best_checkpoint_path = checkpoint_dir / "best_model"
                model.save_pretrained(best_checkpoint_path)
                processor.save_pretrained(best_checkpoint_path)
                print(f"Saved best model to {best_checkpoint_path}")
            else:
                patience_counter += 1
                if patience_counter >= patience:
                    print(f"Early stopping after {patience} epochs without improvement.")
                    break

        # Save final model
        final_model_path = exported_dir / "final_model"
        model.save_pretrained(final_model_path)
        processor.save_pretrained(final_model_path)

        # Save history
        history_path = exported_dir / "history.json"
        history_path.write_text(json.dumps(history, indent=2), encoding="utf-8")

        # Save config
        config_path = exported_dir / "train_config.json"
        persisted_config = asdict(self.config)
        config_path.write_text(json.dumps(persisted_config, indent=2), encoding="utf-8")

        return {
            "model": final_model_path,
            "processor": final_model_path,
            "history": history_path,
            "config": config_path,
            "best_checkpoint": checkpoint_dir / "best_model",
        }
