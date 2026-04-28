param(
    [string]$ModelPath = "models\tflite\handwriting_ocr.tflite",
    [string]$AndroidAssetsDir = "..\android\app\src\main\assets\models"
)

$resolvedModelPath = Resolve-Path -LiteralPath $ModelPath -ErrorAction SilentlyContinue
if (-not $resolvedModelPath) {
    throw "TFLite model not found: $ModelPath"
}

if ([System.IO.Path]::GetExtension($resolvedModelPath.Path).ToLowerInvariant() -ne ".tflite") {
    throw "Expected a .tflite file: $($resolvedModelPath.Path)"
}

$resolvedAssetsDir = Join-Path (Get-Location) $AndroidAssetsDir
New-Item -ItemType Directory -Force -Path $resolvedAssetsDir | Out-Null

$destination = Join-Path $resolvedAssetsDir "handwriting_ocr.tflite"
Copy-Item -LiteralPath $resolvedModelPath.Path -Destination $destination -Force

Write-Host "Copied TFLite model to: $destination"
