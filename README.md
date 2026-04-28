# Illegible Handwriting OCR

This repository has two main parts:

- `android/`: the Android app workspace
- `model_ai/`: the Python training and evaluation workspace

If your goal is to prepare a dataset, train OCR, evaluate improvements, or export a TFLite model, start in `model_ai/`.

## Repository Layout

```text
Enon_Proj/
  android/                     Android application project
  model_ai/                    ML workspace
    .venv/                     Local Python environment
    data/
      raw/                     OCR images + transcripts
      annotations/             Optional metadata for future models
      processed/               Optional preprocessing outputs
    models/
      checkpoints/             Best checkpoints during training
      exported/                Final models, evaluation reports, benchmarks
      tflite/                  Exported TFLite models
    scripts/
      setup_venv.ps1
      inspect_dataset.py
      train_ocr.py
      evaluate.py
      export_tflite.py
```

## Current OCR Workflow

The documented OCR path in this repository is:

1. Put line-level handwriting images and same-stem `.txt` transcripts in `model_ai/data/raw/`.
2. Activate `model_ai/.venv`.
3. Inspect the dataset with `inspect_dataset.py`.
4. Train the OCR baseline with `train_ocr.py`.
5. Evaluate on a fixed validation set with `evaluate.py`.
6. Track improvement through `benchmark_history.json` and `benchmark_history.csv`.
7. Export the trained model with `export_tflite.py`.

## Dataset Placement

Recommended dataset structure:

```text
model_ai/data/raw/
  train/
    line_0001.png
    line_0001.txt
  val/
    line_1001.png
    line_1001.txt
  test/
    line_2001.png
    line_2001.txt
```

Rules:

- one image = one transcript file
- the transcript must be beside the image
- both files must share the same stem
- UTF-8 text is expected
- line-level crops are strongly recommended for this baseline

## Python And Environment

Recommended stable environment:

- Python `3.11.9` 64-bit recommended
- Any Python `3.11.x` 64-bit release is the intended target for this repo
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

The pinned versions live in [model_ai/requirements.txt](/c:/Users/James/Documents/Enon_Proj/model_ai/requirements.txt), and the intended interpreter version lives in [model_ai/.python-version](/c:/Users/James/Documents/Enon_Proj/model_ai/.python-version).

The workspace also has a setup helper at [model_ai/scripts/setup_venv.ps1](/c:/Users/James/Documents/Enon_Proj/model_ai/scripts/setup_venv.ps1).

Install guidance:

- Install Python `3.11.9` 64-bit if you want the exact baseline used for this workspace.
- Python `3.12` may work with TensorFlow `2.16.2`, but the project docs, `.python-version`, and setup flow are standardized on Python `3.11.x`.
- Do not use Python `3.13` for this workspace.

## Use The Existing `.venv`

If the local environment already exists:

```powershell
cd c:\Users\James\Documents\Enon_Proj\model_ai
.\.venv\Scripts\Activate.ps1
python --version
```

If you need to create or repair it:

```powershell
cd c:\Users\James\Documents\Enon_Proj\model_ai
powershell -ExecutionPolicy Bypass -File .\scripts\setup_venv.ps1
.\.venv\Scripts\Activate.ps1
```

If Python is not installed yet, install Python `3.11.9` 64-bit first, then run the commands above.

## End-To-End OCR Example

```powershell
cd c:\Users\James\Documents\Enon_Proj\model_ai
.\.venv\Scripts\Activate.ps1
python scripts\inspect_dataset.py --dataset-root data\raw\train --show-missing
python scripts\train_ocr.py --dataset-root data\raw\train
python scripts\evaluate.py --model models\exported\ocr\final_model.keras --dataset-root data\raw\val --benchmark-name experiment_01 --benchmark-group val_set_v1 --save-predictions
python scripts\export_tflite.py --model models\exported\ocr\final_model.keras --output models\tflite\ocr_model.tflite
```

## Benchmarks

Evaluation now tracks benchmark history automatically.

Files created in `model_ai/models/exported/ocr/`:

- `benchmark_history.json`
- `benchmark_history.csv`

How to use them well:

- keep the validation dataset fixed
- keep the same `--benchmark-group` for comparable runs
- watch for `exact_match_rate` going up
- watch for `character_error_rate` and `word_error_rate` going down

Example benchmarked evaluation:

```powershell
python scripts\evaluate.py --model models\exported\ocr\final_model.keras --dataset-root data\raw\val --benchmark-name experiment_02 --benchmark-group val_set_v1 --save-predictions
```

## Android App

If you want to build the Android app:

```powershell
cd android
.\gradlew.bat assembleDebug
```

Gemini fallback OCR is configured from `android/local.properties` so the API key is not hard-coded:

```properties
geminiApiKey=YOUR_GEMINI_API_KEY
geminiModel=gemini-2.5-flash
geminiTimeoutSeconds=30
```

The Android project is separate from the Python training workflow.

## More Detail

The full ML guide is in [model_ai/README.md](/c:/Users/James/Documents/Enon_Proj/model_ai/README.md).
