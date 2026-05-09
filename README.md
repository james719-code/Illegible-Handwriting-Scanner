# ClearScript

## Description

ClearScript is an Android handwriting OCR project with a companion Python machine learning workspace.

The Android app helps users capture or import handwriting images, preprocess them for readability, run OCR, review extracted text, and manage scan history. The `model_ai/` workspace supports dataset inspection, OCR model training, evaluation, benchmarking, and TensorFlow Lite export.

Gradle project name: `IllegibleHandwritingOCR`.

## Features

- Capture handwriting images with the Android camera.
- Import handwriting images from the device gallery.
- Preprocess images for improved readability.
- Run OCR on handwriting images.
- Review extracted text and confidence-related feedback.
- Manage scan history records.
- Train, evaluate, compare, and export OCR models from the Python workspace.
- Configure Gemini fallback OCR through local Android properties.

## Tech Stack

- Android
  - Kotlin
  - Jetpack Compose
  - Android Gradle Plugin
  - AndroidX Navigation Compose
  - AndroidX CameraX
  - AndroidX WorkManager
  - Android SQLite APIs
  - Coil
  - OkHttp
- Machine Learning / Python
  - Python `3.11.x`
  - TensorFlow `2.16.2`
  - PyTorch `2.2.1`
  - Transformers `4.38.1`
  - OpenCV
  - NumPy
  - pandas
  - scikit-learn
  - Pillow
  - pytesseract

## Installation

### Android App

1. Install Android Studio.
2. Open the `android/` folder as the Android project.
3. Let Gradle sync the project dependencies.
4. Optional: create `android/local.properties` for Gemini OCR configuration. See [Environment Variables](#environment-variables).
5. Build the debug app:

```powershell
cd android
.\gradlew.bat assembleDebug
```

### Python ML Workspace

1. Install Python `3.11.x` 64-bit.
2. Create or repair the local virtual environment:

```powershell
cd model_ai
powershell -ExecutionPolicy Bypass -File .\scripts\setup_venv.ps1
```

3. Activate the virtual environment:

```powershell
.\.venv\Scripts\Activate.ps1
```

4. Verify the installation:

```powershell
python --version
python -m pip show tensorflow numpy pandas opencv-python
```

## Usage

### Android App

Run the app from Android Studio, or build it from the command line:

```powershell
cd android
.\gradlew.bat assembleDebug
```

TODO: Add emulator/device run instructions if this project requires any device-specific setup.

### OCR Training Workflow

Place paired handwriting images and same-stem `.txt` transcript files under `model_ai/data/raw/`.

Recommended dataset layout:

```text
model_ai/data/raw/
  train/
    sample_0001.png
    sample_0001.txt
  val/
    sample_1001.png
    sample_1001.txt
  test/
    sample_2001.png
    sample_2001.txt
```

Run the baseline OCR workflow:

```powershell
cd model_ai
.\.venv\Scripts\Activate.ps1
python scripts\inspect_dataset.py --dataset-root data\raw\train --show-missing
python scripts\train_ocr.py --dataset-root data\raw\train
python scripts\evaluate.py --model models\exported\ocr\final_model.keras --dataset-root data\raw\val --benchmark-name baseline_01 --benchmark-group val_set_v1 --save-predictions
python scripts\export_tflite.py --model models\exported\ocr\final_model.keras --output models\tflite\ocr_model.tflite
```

Run the TrOCR workflow:

```powershell
cd model_ai
.\.venv\Scripts\Activate.ps1
python scripts\train_trocr.py --dataset-root data\raw\train --epochs 25 --batch-size 8
python scripts\evaluate_trocr.py --model models\exported\trocr\final_model --dataset-root data\raw\val
```

## Environment Variables

This project does not document required shell environment variables.

Android OCR fallback settings are read from `android/local.properties`, which should stay local and uncommitted:

```properties
geminiApiKey=YOUR_GEMINI_API_KEY
geminiModel=gemini-2.5-flash
geminiTimeoutSeconds=30
```

Supported fallback property names found in the Android build configuration:

- `geminiApiKey`
- `geminiModel`
- `geminiTimeoutSeconds`
- `defaultAiApiKey`
- `defaultAiTimeoutSeconds`

TODO: Document any production environment variable names if they are added later.

## Project Structure

```text
Enon_Proj/
  android/                     Android application project
    app/
      src/main/java/com/enon/writingai/
        core/                  Shared utilities, image processing, OCR, ML helpers
        data/                  Local data, repositories, mappers, sources
        domain/                Models, repositories, and use cases
        feature/               App screens and view models
        navigation/            Navigation graph and destinations
        ui/                    Theme and shared UI components
      src/main/res/            Android resources
    gradle/                    Gradle wrapper and version catalog
  model_ai/                    Python ML workspace
    data/                      Raw, processed, and annotation folders
    models/                    Checkpoints, exported models, and TFLite outputs
    notebooks/                 Optional experiments
    scripts/                   Training, evaluation, export, and setup scripts
    src/                       OCR, detection, legibility, segmentation, evaluation code
  README.md                    Root project documentation
```

## Scripts

### Android

Run these from the `android/` folder:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat test
.\gradlew.bat connectedAndroidTest
```

### Python

Run these from the `model_ai/` folder after activating `.venv`:

```powershell
python scripts\inspect_dataset.py --dataset-root data\raw\train --show-missing
python scripts\train_ocr.py --dataset-root data\raw\train
python scripts\evaluate.py --model models\exported\ocr\final_model.keras --dataset-root data\raw\val
python scripts\export_tflite.py --model models\exported\ocr\final_model.keras --output models\tflite\ocr_model.tflite
python scripts\train_trocr.py --dataset-root data\raw\train --epochs 25 --batch-size 8
python scripts\evaluate_trocr.py --model models\exported\trocr\final_model --dataset-root data\raw\val
python scripts\compare_models.py --dataset-root data\raw\val --output comparison_report.json
```

PowerShell helper scripts:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup_venv.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\copy_tflite_to_android.ps1
```
