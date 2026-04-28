# Raw Dataset

Place paired OCR samples in this folder. Each handwriting image must have a same-stem `.txt` transcription file.

Example:

```text
sample_001.jpg
sample_001.txt
sample_002.png
sample_002.txt
```

Guidelines:

- Prefer line-level crops when possible
- Keep filenames stable so image/text pairing stays valid
- Store the exact transcription in the `.txt` file
- Large datasets are ignored by default through `.gitignore`
