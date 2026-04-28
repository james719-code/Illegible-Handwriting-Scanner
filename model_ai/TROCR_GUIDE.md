# TrOCR Fine-Tuning Guide

This guide explains how to use the TrOCR (Transformer-based Optical Character Recognition) model to improve upon the baseline CNN OCR model for handwritten text recognition.

## Overview

TrOCR is a Vision Transformer + RoBERTa encoder-decoder architecture pre-trained on handwriting datasets (IAM). Key advantages:

- **Better context modeling**: Transformer attention captures long-range dependencies in text
- **Language understanding**: Pre-trained RoBERTa decoder improves word accuracy
- **Flexible decoding**: Supports beam search for higher accuracy
- **Transfer learning**: Excellent performance with small fine-tuning datasets

## Quick Start

### 1. Prepare Data

Your dataset should be in `model_ai/data/raw/` with this structure:

```
model_ai/data/raw/
├── train/
│   ├── image_001.png
│   ├── image_001.txt  (one line: transcription)
│   ├── image_002.png
│   ├── image_002.txt
│   └── ...
└── val/
    ├── image_101.png
    ├── image_101.txt
    └── ...
```

### 2. Train TrOCR

```bash
cd model_ai
python scripts/train_trocr.py \
  --dataset-root data/raw/train \
  --epochs 25 \
  --batch-size 8 \
  --learning-rate 1e-5
```

**Key Parameters:**

| Parameter | Default | Description |
|-----------|---------|-------------|
| `--dataset-root` | `data/raw/train` | Training data directory |
| `--epochs` | 25 | Number of training epochs |
| `--batch-size` | 8 | Batch size (adjust based on GPU memory) |
| `--learning-rate` | 1e-5 | AdamW learning rate |
| `--freeze-encoder` | False | Freeze vision transformer encoder |
| `--model-name` | `microsoft/trocr-base-handwritten` | HuggingFace model ID |
| `--device` | `cuda` | Device (`cuda` or `cpu`) |

**For Small Datasets (<500 samples):**
```bash
python scripts/train_trocr.py \
  --dataset-root data/raw/train \
  --epochs 50 \
  --batch-size 4 \
  --learning-rate 5e-6 \
  --freeze-encoder
```

**Output:** Trained model saved to `models/exported/trocr/final_model/`

### 3. Evaluate TrOCR

```bash
python scripts/evaluate_trocr.py \
  --model models/exported/trocr/final_model \
  --dataset-root data/raw/val
```

**Output:** Metrics (CER, WER, exact match rate) + benchmark history update

### 4. Compare with Baseline

```bash
python scripts/compare_models.py \
  --dataset-root data/raw/val \
  --output comparison_report.json
```

This generates a side-by-side comparison showing:
- Exact Match Rate improvement
- Character Error Rate (CER) reduction
- Word Error Rate (WER) reduction

## Architecture Details

### Model: microsoft/trocr-base-handwritten

**Encoder (Vision Transformer):**
- Input: 384×384 RGB images
- Architecture: DeiT base model
- Output: 768-dimensional visual tokens

**Decoder (Language Model):**
- Pre-trained RoBERTa
- Autoregressive text generation
- Supports beam search decoding

**Parameters:** ~300M (encoder) + ~130M (decoder)

### Training Pipeline

```
Raw Images (variable size)
    ↓
[Resize to 384×384, normalize]
    ↓
[Augmentation: rotation, blur, brightness, elastic distortion]
    ↓
[TrOCRProcessor: tokenize images + texts]
    ↓
[DataLoader: batch collation]
    ↓
[TrOCRTrainer: forward pass, loss backprop]
    ↓
[Optimizer: AdamW + ReduceLROnPlateau]
    ↓
[Checkpoint best model + early stopping]
```

### Data Augmentation

During training, images are augmented with:

- **Rotation**: ±15 degrees
- **Blur**: up to 3 pixels
- **Brightness/Contrast**: ±20%
- **Elastic Distortion**: for pen stroke variation
- **Perspective Transform**: ±10% skew
- **Gaussian Noise**: up to 2% intensity

These simulate real handwriting variations.

## Performance Tuning

### For Higher Accuracy (GPU with 8GB+ memory):

```bash
python scripts/train_trocr.py \
  --dataset-root data/raw/train \
  --epochs 50 \
  --batch-size 16 \
  --learning-rate 2e-5 \
  --model-name microsoft/trocr-base-handwritten
```

### For Smaller Models (if accuracy is sufficient):

```bash
python scripts/train_trocr.py \
  --dataset-root data/raw/train \
  --epochs 40 \
  --batch-size 8 \
  --learning-rate 1e-5 \
  --model-name microsoft/trocr-small-handwritten
```

`trocr-small` is 40% smaller but ~2% less accurate.

### For Limited Data (<100 samples):

```bash
python scripts/train_trocr.py \
  --dataset-root data/raw/train \
  --epochs 100 \
  --batch-size 2 \
  --learning-rate 5e-6 \
  --freeze-encoder  # Freeze vision transformer
```

**Why freeze encoder:** Reduces overfitting by only fine-tuning the lightweight decoder.

## Usage in Code

### Simple Prediction:

```python
from src.ocr.trocr_inference import predict_text_trocr
from PIL import Image

image = Image.open("handwritten_page.png")
prediction = predict_text_trocr(
    model_path="models/exported/trocr/final_model",
    image=image,
    device="cuda"
)
print(prediction)  # "your transcribed text"
```

### Batch Prediction:

```python
from src.ocr.trocr_inference import predict_batch_trocr

predictions = predict_batch_trocr(
    model_path="models/exported/trocr/final_model",
    image_paths=["img1.png", "img2.png", "img3.png"],
    device="cuda",
    beam_search=True  # More accurate but slower
)

for path, text in predictions.items():
    print(f"{path}: {text}")
```

## Troubleshooting

### Out of Memory (OOM)

Reduce batch size and/or freeze encoder:
```bash
python scripts/train_trocr.py \
  --batch-size 4 \
  --freeze-encoder
```

### Low Accuracy After Training

1. **More data needed**: TrOCR benefits from >500 training samples
2. **Augmentation too strong**: Edit `src/ocr/trocr_data.py` to reduce rotation/distortion
3. **Learning rate too high**: Try `--learning-rate 5e-6`
4. **Train longer**: Increase `--epochs` to 50+

### Slow Training

1. Use `--freeze-encoder` to train only the decoder (3-4x faster)
2. Reduce `--batch-size` to fit in GPU memory
3. Use `microsoft/trocr-small-handwritten` for lighter model

## Comparison with Baseline

The baseline CNN model:
- **Pros**: Fast inference, small model size, runs on CPU
- **Cons**: Fixed-length context, limited language understanding

TrOCR:
- **Pros**: Variable-length text, context-aware, better on cursive/difficult text
- **Cons**: Slower inference, larger model, GPU recommended

**Expected improvement:** 5-20% error rate reduction depending on your data.

## Model Export and Deployment

### PyTorch Format (Current)

Model saved by default to `models/exported/trocr/final_model/`:
- `pytorch_model.bin`: Model weights
- `config.json`: Architecture config
- `preprocessor_config.json`: Image processor settings
- `tokenizer_config.json`: Text tokenizer settings
- `special_tokens_map.json`: Special token mappings

### ONNX Export (Future)

For production deployment, TrOCR can be exported to ONNX format for inference on any platform:

```bash
# (This will be added in a future update)
python scripts/export_trocr_onnx.py \
  --model models/exported/trocr/final_model \
  --output models/exported/trocr/model.onnx
```

## References

- **TrOCR Paper**: https://arxiv.org/abs/2109.10282
- **HuggingFace Models**: https://huggingface.co/microsoft/trocr-base-handwritten
- **Transformers Docs**: https://huggingface.co/docs/transformers/

## File Structure

```
model_ai/
├── src/ocr/
│   ├── trocr_data.py       # Dataset & DataLoader
│   ├── trocr_train.py      # Fine-tuning trainer
│   ├── trocr_inference.py  # Prediction functions
│   └── (baseline model.py already exists)
├── scripts/
│   ├── train_trocr.py      # CLI for training
│   ├── evaluate_trocr.py   # Evaluation script
│   ├── compare_models.py   # Baseline vs TrOCR comparison
│   └── (existing scripts)
├── models/
│   └── exported/
│       ├── ocr/            # Baseline model
│       └── trocr/          # TrOCR model
└── data/
    └── raw/
        ├── train/          # Training images + transcripts
        └── val/            # Validation images + transcripts
```

---

**Questions?** Check the test script: `python scripts/test_trocr_pipeline.py`
