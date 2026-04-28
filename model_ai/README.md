# Illegible Handwriting OCR Training Workspace

This folder contains the Python training workspace for the project.

The runnable workflow currently centers on paired OCR samples:

- one handwriting image
- one same-stem `.txt` file containing the exact transcription for that image

The baseline is simplest and most reliable when each image contains a single text line.

## What You Can Do Here

This workspace currently supports:

- inspecting an OCR dataset before training
- training a TensorFlow OCR baseline from image + text pairs
- evaluating a trained `.keras` OCR model
- exporting a trained OCR model to `.tflite`
- tracking benchmark history across repeated evaluation runs

There is also code under `src/detection/`, `src/legibility/`, and `src/segmentation/`, but the end-to-end documented workflow in this folder is the OCR path above.

## Folder Layout

```text
model_ai/
  .venv/                      Local Python virtual environment
  .python-version             Intended Python version for this workspace
  data/
    raw/                      Place OCR image + transcript pairs here
    annotations/              Optional metadata / manifests
    processed/                Optional preprocessing outputs
  models/
    checkpoints/              Best checkpoints during training
    exported/                 Final .keras models, reports, benchmark files
    tflite/                   Exported .tflite models
  notebooks/                  Optional experiments
  scripts/
    setup_venv.ps1
    inspect_dataset.py
    train_ocr.py
    evaluate.py
    export_tflite.py
  src/
    ocr/
    evaluation/
```

## Dataset Format

Put your OCR dataset under `data/raw/`.

Recommended split layout:

```text
data/raw/
  train/
    sample_0001.png
    sample_0001.txt
    sample_0002.jpg
    sample_0002.txt
  val/
    sample_1001.png
    sample_1001.txt
  test/
    sample_2001.png
    sample_2001.txt
```

Nested folders are allowed:

```text
data/raw/train/writer_01/page_03/line_001.png
data/raw/train/writer_01/page_03/line_001.txt
```

Rules:

- Every image must have a `.txt` file with the same filename stem in the same folder.
- Text files must be UTF-8.
- Empty transcript files will fail loading.
- At least 2 paired samples are required to train because the code creates a train/validation split.
- Line-level crops work much better than full-page images for this baseline.
- If you create processed images in `data/processed/`, keep the same stem so the transcript still maps correctly.

Example valid pair:

```text
sample_0001.png
sample_0001.txt
```

Example transcript content:

```text
The quick brown fox jumps over the lazy dog.
```

Incorrect naming example:

```text
sample_0001.png
sample_0001_label.txt
```

## Environment And Python Version

Recommended stable baseline for this workspace:

- Python `3.11.9` 64-bit recommended
- Any Python `3.11.x` 64-bit release is the intended target for this workspace
- TensorFlow `2.16.2`
- NumPy `1.26.4`
- pandas `2.2.3`
- OpenCV `4.10.0.84`
- Pillow `10.4.0`
- scikit-learn `1.5.2`
- matplotlib `3.9.2`
- PyYAML `6.0.2`
- tqdm `4.67.1`
- pytesseract `0.3.13`

These versions are pinned in `requirements.txt`, and `.python-version` points to `3.11`.

If you want to use the `pytesseract` inference helpers, install native Tesseract OCR separately on the machine. The Python package alone is not enough for that path.

Install recommendation:

- Install Python `3.11.9` 64-bit if you want the exact environment this workspace was set up around.
- Python `3.12` is within TensorFlow `2.16.2` support, but this project standardizes on Python `3.11.x`.
- Avoid Python `3.13` for this workspace.
- After installation, `python --version` should report `3.11.x`.

## Virtual Environment

This workspace uses `model_ai/.venv` as the local Python environment.

If `.venv` already exists, activate it:

```powershell
cd c:\Users\James\Documents\Enon_Proj\model_ai
.\.venv\Scripts\Activate.ps1
```

If activation is blocked in PowerShell:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\.venv\Scripts\Activate.ps1
```

Command Prompt alternative:

```bat
.\.venv\Scripts\activate.bat
```

Verify the environment:

```powershell
python --version
python -m pip show tensorflow numpy pandas opencv-python
```

## Create Or Recreate The Environment

If `.venv` is missing or you want to rebuild it, run:

```powershell
cd c:\Users\James\Documents\Enon_Proj\model_ai
powershell -ExecutionPolicy Bypass -File .\scripts\setup_venv.ps1
```

Prerequisite:

- install Python `3.11.9` 64-bit before running the setup script

That script:

- creates `.venv`
- upgrades `pip`, `setuptools`, and `wheel`
- installs the pinned packages from `requirements.txt`

Manual alternative:

```powershell
py -3.11 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip setuptools wheel
pip install -r requirements.txt
```

## Quick Start

If the environment is already ready, this is the shortest OCR flow:

```powershell
cd c:\Users\James\Documents\Enon_Proj\model_ai
.\.venv\Scripts\Activate.ps1
python scripts\inspect_dataset.py --dataset-root data\raw\train --show-missing
python scripts\train_ocr.py --dataset-root data\raw\train
python scripts\evaluate.py --model models\exported\ocr\final_model.keras --dataset-root data\raw\val --benchmark-name baseline_01 --benchmark-group val_set_v1 --save-predictions
python scripts\export_tflite.py --model models\exported\ocr\final_model.keras --output models\tflite\ocr_model.tflite
```

## Full Guide From Scratch

### 1. Open the workspace

```powershell
cd c:\Users\James\Documents\Enon_Proj\model_ai
```

### 2. Create or repair the environment

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup_venv.ps1
```

### 3. Activate the environment

```powershell
.\.venv\Scripts\Activate.ps1
```

### 4. Put your dataset in place

Recommended:

```text
data/raw/
  train/
  val/
  test/
```

Use `train/` for training data, `val/` for repeatable model comparison, and `test/` for final checks if you want a separate held-out set.

### 5. Inspect the training dataset

```powershell
python scripts\inspect_dataset.py --dataset-root data\raw\train --show-missing
```

### 6. Train the OCR model

```powershell
python scripts\train_ocr.py --dataset-root data\raw\train
```

### 7. Evaluate on the same validation benchmark each time

```powershell
python scripts\evaluate.py --model models\exported\ocr\final_model.keras --dataset-root data\raw\val --benchmark-name experiment_01 --benchmark-group val_set_v1 --save-predictions
```

### 8. Export to TFLite

```powershell
python scripts\export_tflite.py --model models\exported\ocr\final_model.keras --output models\tflite\ocr_model.tflite
```

### 9. Leave the environment

```powershell
deactivate
```

## Command Reference

### Inspect Dataset

Basic:

```powershell
python scripts\inspect_dataset.py --dataset-root data\raw\train --show-missing
```

Useful variant:

```powershell
python scripts\inspect_dataset.py --dataset-root data\raw\train --show-missing --limit 25
```

What it tells you:

- number of valid image/text pairs
- images missing transcript files
- maximum text length
- average text length
- character set preview

### Train OCR

Basic:

```powershell
python scripts\train_ocr.py --dataset-root data\raw\train
```

Explicit example:

```powershell
python scripts\train_ocr.py --dataset-root data\raw\train --epochs 25 --image-width 256 --image-height 64 --batch-size 8 --validation-split 0.2 --learning-rate 0.001
```

Useful options:

- `--dataset-root data/raw/train`
- `--output-root models`
- `--image-width 256`
- `--image-height 64`
- `--batch-size 8`
- `--epochs 25`
- `--validation-split 0.2`
- `--learning-rate 0.001`
- `--max-text-length 80`
- `--max-samples 500`
- `--lowercase`

Important behavior:

- The trainer scans the folder you pass recursively.
- It creates its own internal train/validation split from that folder.
- If you already have separate `train/`, `val/`, and `test/` folders, point training at `data/raw/train` only.
- There is no dedicated resume-training CLI yet; a new training run overwrites the standard exported OCR artifacts.

### Evaluate OCR

Basic validation run:

```powershell
python scripts\evaluate.py --model models\exported\ocr\final_model.keras --dataset-root data\raw\val --save-predictions
```

Recommended benchmarked run:

```powershell
python scripts\evaluate.py --model models\exported\ocr\final_model.keras --dataset-root data\raw\val --benchmark-name experiment_07 --benchmark-group val_set_v1 --save-predictions
```

Alternate output file:

```powershell
python scripts\evaluate.py --model models\exported\ocr\final_model.keras --dataset-root data\raw\test --output models\exported\ocr\test_evaluation.json --benchmark-name test_run_01 --benchmark-group test_set_v1 --save-predictions
```

Useful options:

- `--model models/exported/ocr/final_model.keras`
- `--dataset-root data/raw/val`
- `--output models/exported/ocr/evaluation.json`
- `--benchmark-name experiment_07`
- `--benchmark-group val_set_v1`
- `--benchmark-history models/exported/ocr/benchmark_history.json`
- `--save-predictions`
- `--max-samples 100`
- `--image-width 256`
- `--image-height 64`
- `--max-text-length 80`
- `--lowercase`
- `--skip-benchmark`

Evaluation output includes:

- exact match rate
- character error rate
- word error rate
- a small preview of prediction vs reference

### Export To TFLite

Standard export:

```powershell
python scripts\export_tflite.py --model models\exported\ocr\final_model.keras --output models\tflite\ocr_model.tflite
```

Quantized export:

```powershell
python scripts\export_tflite.py --model models\exported\ocr\final_model.keras --output models\tflite\ocr_model_quant.tflite --quantize
```

Optimized export modes:

```powershell
python scripts\export_tflite.py --model models\exported\ocr\final_model.keras --output models\tflite\ocr_model_dynamic.tflite --optimization dynamic
python scripts\export_tflite.py --model models\exported\ocr\final_model.keras --output models\tflite\ocr_model_float16.tflite --optimization float16
```

The exporter validates the input/output paths and prints the selected optimization mode, source size, output size, and output/input ratio after conversion.

## Benchmark Tracking

Benchmark tracking is built into `scripts/evaluate.py`.

Every normal evaluation run now updates:

- `models/exported/ocr/benchmark_history.json`
- `models/exported/ocr/benchmark_history.csv`

What is tracked per run:

- timestamp
- benchmark name
- benchmark group
- dataset path
- model path
- report path
- sample count
- exact match rate
- character error rate
- word error rate

How comparison works:

- runs are compared within the same `benchmark_group`
- if you do not pass `--benchmark-group`, the resolved dataset path is used
- the current run is compared against the previous run from the same group
- the evaluation report also stores the benchmark summary

Best practice for meaningful benchmarks:

1. Keep one fixed validation folder such as `data/raw/val`.
2. Use the same `--benchmark-group` for all experiments on that validation set.
3. Change only one thing at a time when possible.
4. Compare runs using the same dataset and the same evaluation settings.

Example sequence:

```powershell
python scripts\train_ocr.py --dataset-root data\raw\train --epochs 20
python scripts\evaluate.py --model models\exported\ocr\final_model.keras --dataset-root data\raw\val --benchmark-name exp_01 --benchmark-group val_set_v1 --save-predictions

python scripts\train_ocr.py --dataset-root data\raw\train --epochs 30 --lowercase
python scripts\evaluate.py --model models\exported\ocr\final_model.keras --dataset-root data\raw\val --benchmark-name exp_02 --benchmark-group val_set_v1 --save-predictions
```

How to tell whether the model is getting better:

- `exact_match_rate` should go up
- `character_error_rate` should go down
- `word_error_rate` should go down

When benchmark comparisons are not reliable:

- you changed the validation dataset
- you compare different benchmark groups
- you changed preprocessing or text normalization in a way that makes runs incomparable

## Output Files

Main OCR artifacts are written to:

```text
models/checkpoints/ocr/
models/exported/ocr/
models/tflite/
```

Important files:

- `models/checkpoints/ocr/best_model.keras`
- `models/exported/ocr/final_model.keras`
- `models/exported/ocr/history.json`
- `models/exported/ocr/train_config.json`
- `models/exported/ocr/vocabulary.json`
- `models/exported/ocr/sample_predictions.json`
- `models/exported/ocr/evaluation.json`
- `models/exported/ocr/benchmark_history.json`
- `models/exported/ocr/benchmark_history.csv`
- `models/tflite/ocr_model.tflite`

## Suggested Workflow

Use this pattern for repeatable progress:

1. Keep a stable training folder in `data/raw/train`.
2. Keep a stable benchmark folder in `data/raw/val`.
3. Run `inspect_dataset.py` before training on new data.
4. Train with `train_ocr.py`.
5. Evaluate with the same benchmark group name every time.
6. Review the benchmark deltas after each run.
7. Export `.tflite` only after you are satisfied with the benchmark trend.

## Troubleshooting

### `.venv` exists but activation or Python fails

Rebuild it:

```powershell
Remove-Item -Recurse -Force .\.venv
powershell -ExecutionPolicy Bypass -File .\scripts\setup_venv.ps1
```

### PowerShell blocks `Activate.ps1`

Use:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\.venv\Scripts\Activate.ps1
```

### The inspector reports missing transcripts

Make sure every image has a same-stem `.txt` file in the same folder.

### Training quality is poor

Try these first:

- crop to single text lines
- remove uncertain labels
- keep orientation consistent
- use cleaner scans
- keep the benchmark set fixed so you can trust the trend

### Full-page images perform badly

That is expected with this simple baseline. Segment pages into lines first, then train OCR on line images.

## TrOCR: Transformer-Based OCR (Improved Accuracy)

The baseline CNN model is simple and reliable, but TrOCR (Transformer-based OCR) can achieve **5-20% error rate reduction** with the same dataset.

### Why TrOCR?

- **Better context**: Vision Transformer + RoBERTa language model captures long-range dependencies
- **Pre-trained**: Fine-tuned on IAM handwriting dataset
- **Variable-length**: Handles any text length, not fixed-size
- **Language awareness**: Improves word-level accuracy

### Quick Start with TrOCR

**For a complete guide, see [TROCR_GUIDE.md](TROCR_GUIDE.md)**

Train TrOCR on your dataset:

```powershell
python scripts\train_trocr.py --dataset-root data\raw\train --epochs 25 --batch-size 8
```

Evaluate on validation set:

```powershell
python scripts\evaluate_trocr.py --model models\exported\trocr\final_model --dataset-root data\raw\val
```

Compare baseline vs TrOCR:

```powershell
python scripts\compare_models.py --dataset-root data\raw\val --output comparison_report.json
```

### TrOCR vs Baseline

| Aspect | Baseline CNN | TrOCR |
|--------|-------------|-------|
| **Accuracy** | Good (baseline) | Better (+5-20% on difficult text) |
| **Context** | Fixed length | Variable length |
| **Speed** | Very fast | Slower (~2-5x) |
| **Model size** | ~2 MB | ~450 MB |
| **GPU required** | No | Recommended |
| **Setup time** | Minutes | Minutes |
| **Fine-tuning data** | Minimum 50 samples | Optimal 200+ samples |

### Next Steps

1. Keep both models: baseline for production (small, fast), TrOCR for accuracy
2. A/B test: evaluate both on validation set using `compare_models.py`
3. If TrOCR wins, use it for critical documents; keep baseline for speed-critical scenarios
4. Future: convert TrOCR to ONNX for broader deployment

### Troubleshooting TrOCR

**Out of memory?** Reduce batch size and freeze encoder:
```powershell
python scripts\train_trocr.py --dataset-root data\raw\train --batch-size 4 --freeze-encoder
```

**No improvement?** Needs more data or different hyperparameters:
```powershell
python scripts\train_trocr.py --dataset-root data\raw\train --epochs 50 --learning-rate 5e-6 --freeze-encoder
```

**Want the full guide?** See [TROCR_GUIDE.md](TROCR_GUIDE.md) for detailed parameters, architecture, and production deployment.

## Notes

- The baseline is intentionally simple so the pipeline is easy to understand and run.
- It is a practical starting point, not a state-of-the-art handwriting OCR architecture.
- TrOCR is available as an improved alternative with the same dataset layout.
- You can use both models together: baseline for production speed, TrOCR for maximum accuracy.
