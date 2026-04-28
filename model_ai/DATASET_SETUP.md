# Dataset Setup Guide for TrOCR Training

## Quick Start: Create Test Data

If you want to quickly test the training pipeline, create some sample data:

```powershell
cd C:\Users\James\Documents\Enon_Proj\model_ai
mkdir -p data\raw\train
mkdir -p data\raw\val

# Create a simple test image with text
python -c "
from PIL import Image, ImageDraw, ImageFont
import pathlib

# Create training samples
for i in range(5):
    img = Image.new('RGB', (256, 64), color='white')
    draw = ImageDraw.Draw(img)
    text = f'Sample text {i}'
    draw.text((10, 20), text, fill='black')
    img.save(f'data/raw/train/sample_{i:04d}.png')
    pathlib.Path(f'data/raw/train/sample_{i:04d}.txt').write_text(text)

# Create validation samples
for i in range(5, 8):
    img = Image.new('RGB', (256, 64), color='white')
    draw = ImageDraw.Draw(img)
    text = f'Validation sample {i}'
    draw.text((10, 20), text, fill='black')
    img.save(f'data/raw/val/sample_{i:04d}.png')
    pathlib.Path(f'data/raw/val/sample_{i:04d}.txt').write_text(text)
"

# Now train
python scripts\train_trocr.py --dataset-root data\raw\train --epochs 3 --batch-size 2 --max-samples 5
```

## Real Dataset Setup

For your actual handwriting dataset:

### Directory Structure

```
data/raw/
├── train/
│   ├── handwriting_001.png
│   ├── handwriting_001.txt  (contains: "transcription text")
│   ├── handwriting_002.png
│   ├── handwriting_002.txt
│   └── ... (more pairs)
└── val/
    ├── handwriting_101.png
    ├── handwriting_101.txt
    └── ... (more pairs)
```

### Requirements

- **Image format**: PNG, JPG, or other PIL-supported formats
- **Text format**: UTF-8 plain text, one line per file
- **Pairing**: Each image must have a `.txt` file with the exact same stem name
- **Minimum**: At least 2 training pairs (1 train + 1 validation split)
- **Recommended**: 200+ training samples for good accuracy

### Text File Format

Each `.txt` file should contain ONE line with the exact transcription:

**File**: `handwriting_001.txt  
**Content**: `The quick brown fox jumps over the lazy dog`

✓ Correct - single line with transcription  
✗ Wrong - multiple lines, empty file, or with metadata

### Image Recommendations

- **Size**: Line-level crops work best (not full pages)
- **Resolution**: 256×64 or 384×384 works well
- **Orientation**: Consistent left-to-right
- **Quality**: Clean scans or photos, good contrast
- **Length**: 5-100 characters per line

### Example Folder Structure

```
data/raw/train/
├── writer_01/
│   ├── page_01/
│   │   ├── line_001.png
│   │   ├── line_001.txt (content: "First line of text")
│   │   ├── line_002.png
│   │   ├── line_002.txt (content: "Second line of text")
│   └── page_02/
│       └── ...
├── writer_02/
│   └── ...
```

The script recursively searches all subdirectories, so nested organization is fine.

## Training Once Data is Ready

```powershell
cd C:\Users\James\Documents\Enon_Proj\model_ai
.\.venv\Scripts\Activate.ps1

# Simple training
python scripts\train_trocr.py --dataset-root data\raw\train --epochs 25 --batch-size 8

# For small datasets (< 100 samples)
python scripts\train_trocr.py --dataset-root data\raw\train --epochs 50 --batch-size 4 --freeze-encoder

# For large datasets (> 500 samples)  
python scripts\train_trocr.py --dataset-root data\raw\train --epochs 15 --batch-size 16

# Evaluate
python scripts\evaluate_trocr.py --model models\exported\trocr\final_model --dataset-root data\raw\val

# Compare with baseline (if you have a baseline trained)
python scripts\compare_models.py --dataset-root data\raw\val
```

## Testing the Setup

Verify your dataset is correct:

```powershell
python scripts\inspect_dataset.py --dataset-root data\raw\train --show-missing --limit 10
```

This shows:
- Number of valid pairs
- Character set
- Average text length
- Any missing files

## Troubleshooting

### "No image + transcript pairs were found"
- Check that files are named: `image_name.png` AND `image_name.txt` (same stem)
- Verify `.txt` files are not empty
- Ensure both are in the same folder

### "Empty transcript files will fail loading"
- Each `.txt` file must contain at least one character
- Transcripts should not have leading/trailing newlines

### File extension issues
- TrOCR accepts: `.png`, `.jpg`, `.jpeg`, `.tiff`, `.bmp`
- Text files must be `.txt`
- Use correct case for extensions (`.PNG` vs `.png`)

### Out of memory during training
- Reduce batch size: `--batch-size 4`
- Freeze encoder: `--freeze-encoder`
- Reduce image size (edit `trocr_data.py` to use smaller size)

---

**Next step**: Place your handwriting dataset in `data/raw/train/` and `data/raw/val/`, then run the training command above.
