from __future__ import annotations

import argparse
import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
SRC_ROOT = PROJECT_ROOT / "src"
if str(SRC_ROOT) not in sys.path:
    sys.path.insert(0, str(SRC_ROOT))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Inspect a paired OCR dataset made of image files and .txt transcripts.")
    parser.add_argument(
        "--dataset-root",
        "--dataset-dir",
        dest="dataset_root",
        default=str(PROJECT_ROOT / "data" / "raw"),
    )
    parser.add_argument("--show-missing", action="store_true", help="Print images that do not have matching .txt files.")
    parser.add_argument("--limit", type=int, default=10, help="How many missing files to print.")
    return parser.parse_args()


def main() -> None:
    args = parse_args()

    from ocr.data import inspect_dataset_root

    summary = inspect_dataset_root(args.dataset_root)
    charset_preview = "".join(summary["charset"][:40])

    print(f"Dataset root: {Path(args.dataset_root).resolve()}")
    print(f"Paired samples: {summary['pair_count']}")
    print(f"Missing transcripts: {summary['missing_count']}")
    print(f"Max text length: {summary['max_text_length']}")
    print(f"Average text length: {summary['average_text_length']:.2f}")
    print(f"Unique characters: {len(summary['charset'])}")
    print(f"Charset preview: {charset_preview}")

    if args.show_missing and summary["missing_transcripts"]:
        print("\nMissing transcript files:")
        for path in summary["missing_transcripts"][:args.limit]:
            print(path)


if __name__ == "__main__":
    main()
