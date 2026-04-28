from __future__ import annotations

import csv
import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


BENCHMARK_FIELDS = [
    "timestamp_utc",
    "benchmark_name",
    "benchmark_group",
    "dataset_root",
    "model_path",
    "report_path",
    "sample_count",
    "exact_match_rate",
    "character_error_rate",
    "word_error_rate",
]


def load_benchmark_history(history_path: str | Path) -> list[dict[str, Any]]:
    history_path = Path(history_path)
    if not history_path.exists():
        return []

    payload = json.loads(history_path.read_text(encoding="utf-8"))
    if not isinstance(payload, list):
        raise ValueError(f"Benchmark history must be a JSON list: {history_path}")
    return payload


def save_benchmark_history(history: list[dict[str, Any]], history_path: str | Path) -> Path:
    history_path = Path(history_path)
    history_path.parent.mkdir(parents=True, exist_ok=True)
    history_path.write_text(json.dumps(history, indent=2, ensure_ascii=False), encoding="utf-8")
    return history_path


def save_benchmark_csv(history: list[dict[str, Any]], csv_path: str | Path) -> Path:
    csv_path = Path(csv_path)
    csv_path.parent.mkdir(parents=True, exist_ok=True)
    with csv_path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=BENCHMARK_FIELDS)
        writer.writeheader()
        for entry in history:
            writer.writerow({field: entry.get(field, "") for field in BENCHMARK_FIELDS})
    return csv_path


def create_ocr_benchmark_entry(
    report: dict[str, Any],
    *,
    model_path: str | Path,
    dataset_root: str | Path,
    report_path: str | Path,
    benchmark_name: str | None = None,
    benchmark_group: str | None = None,
) -> dict[str, Any]:
    dataset_root = Path(dataset_root).resolve()
    model_path = Path(model_path).resolve()
    report_path = Path(report_path).resolve()

    inferred_name = benchmark_name or model_path.stem
    inferred_group = benchmark_group or str(dataset_root)

    return {
        "timestamp_utc": datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z"),
        "benchmark_name": inferred_name,
        "benchmark_group": inferred_group,
        "dataset_root": str(dataset_root),
        "model_path": str(model_path),
        "report_path": str(report_path),
        "sample_count": int(report["sample_count"]),
        "exact_match_rate": float(report["exact_match_rate"]),
        "character_error_rate": float(report["character_error_rate"]),
        "word_error_rate": float(report["word_error_rate"]),
    }


def _comparison_payload(current: dict[str, Any], reference: dict[str, Any]) -> dict[str, Any]:
    exact_delta = float(current["exact_match_rate"]) - float(reference["exact_match_rate"])
    cer_delta = float(current["character_error_rate"]) - float(reference["character_error_rate"])
    wer_delta = float(current["word_error_rate"]) - float(reference["word_error_rate"])
    return {
        "benchmark_name": reference["benchmark_name"],
        "timestamp_utc": reference["timestamp_utc"],
        "sample_count": int(reference["sample_count"]),
        "exact_match_rate": float(reference["exact_match_rate"]),
        "character_error_rate": float(reference["character_error_rate"]),
        "word_error_rate": float(reference["word_error_rate"]),
        "delta_exact_match_rate": exact_delta,
        "delta_character_error_rate": cer_delta,
        "delta_word_error_rate": wer_delta,
        "improved_exact_match_rate": exact_delta > 0,
        "improved_character_error_rate": cer_delta < 0,
        "improved_word_error_rate": wer_delta < 0,
    }


def summarize_ocr_benchmark(history: list[dict[str, Any]], current: dict[str, Any]) -> dict[str, Any]:
    comparable_runs = [
        entry
        for entry in history
        if entry.get("benchmark_group") == current["benchmark_group"]
    ]

    previous_run = comparable_runs[-2] if len(comparable_runs) >= 2 else None
    best_exact_match = max(comparable_runs, key=lambda entry: float(entry["exact_match_rate"]))
    best_character_error = min(comparable_runs, key=lambda entry: float(entry["character_error_rate"]))
    best_word_error = min(comparable_runs, key=lambda entry: float(entry["word_error_rate"]))

    return {
        "benchmark_group": current["benchmark_group"],
        "comparable_run_count": len(comparable_runs),
        "previous_run": _comparison_payload(current, previous_run) if previous_run else None,
        "best_so_far": {
            "exact_match_rate": {
                "benchmark_name": best_exact_match["benchmark_name"],
                "timestamp_utc": best_exact_match["timestamp_utc"],
                "value": float(best_exact_match["exact_match_rate"]),
                "is_current_best": best_exact_match["timestamp_utc"] == current["timestamp_utc"],
            },
            "character_error_rate": {
                "benchmark_name": best_character_error["benchmark_name"],
                "timestamp_utc": best_character_error["timestamp_utc"],
                "value": float(best_character_error["character_error_rate"]),
                "is_current_best": best_character_error["timestamp_utc"] == current["timestamp_utc"],
            },
            "word_error_rate": {
                "benchmark_name": best_word_error["benchmark_name"],
                "timestamp_utc": best_word_error["timestamp_utc"],
                "value": float(best_word_error["word_error_rate"]),
                "is_current_best": best_word_error["timestamp_utc"] == current["timestamp_utc"],
            },
        },
    }


def format_ocr_benchmark_summary(summary: dict[str, Any], current: dict[str, Any]) -> list[str]:
    lines = [
        f"Benchmark group: {summary['benchmark_group']}",
        f"Runs on this benchmark: {summary['comparable_run_count']}",
        (
            "Current metrics: "
            f"exact_match_rate={current['exact_match_rate']:.4f}, "
            f"character_error_rate={current['character_error_rate']:.4f}, "
            f"word_error_rate={current['word_error_rate']:.4f}"
        ),
    ]

    previous_run = summary.get("previous_run")
    if previous_run is None:
        lines.append("No previous comparable benchmark yet.")
    else:
        lines.append(
            "Vs previous: "
            f"exact_match_rate={previous_run['delta_exact_match_rate']:+.4f}, "
            f"character_error_rate={previous_run['delta_character_error_rate']:+.4f}, "
            f"word_error_rate={previous_run['delta_word_error_rate']:+.4f}"
        )

    best_so_far = summary["best_so_far"]
    lines.append(
        "Best so far: "
        f"exact_match_rate={best_so_far['exact_match_rate']['value']:.4f}, "
        f"character_error_rate={best_so_far['character_error_rate']['value']:.4f}, "
        f"word_error_rate={best_so_far['word_error_rate']['value']:.4f}"
    )
    return lines
