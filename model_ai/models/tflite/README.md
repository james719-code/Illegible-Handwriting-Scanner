# TFLite Models

Converted OCR inference `.tflite` models for Android deployment are written here.

The Android app looks for the OCR model at:

```text
android/app/src/main/assets/models/handwriting_ocr.tflite
```

After exporting a compatible `.tflite` model, copy it into the app with:

```powershell
cd model_ai
.\scripts\copy_tflite_to_android.ps1 -ModelPath .\models\tflite\handwriting_ocr.tflite
```

Note: the current trained TrOCR artifact in `models/exported/trocr/final_model` is a Hugging Face/PyTorch `VisionEncoderDecoderModel` saved as `model.safetensors`. The existing `scripts/export_tflite.py` converter only accepts Keras `.keras` models, so that TrOCR artifact cannot be exported by the current converter without adding a separate PyTorch/ONNX/AI Edge conversion pipeline.
