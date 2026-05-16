param(
    [string]$VenvPath = "",
    [switch]$SkipInstall,
    [switch]$Recreate
)

$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $PSScriptRoot
if (-not $VenvPath) {
    $VenvPath = Join-Path $ProjectRoot ".venv"
}

$RequirementsPath = Join-Path $ProjectRoot "requirements.txt"
$VenvPython = Join-Path $VenvPath "Scripts\\python.exe"
$ActivateScript = Join-Path $VenvPath "Scripts\\Activate.ps1"

function Get-PythonLauncher {
    if (Get-Command py -ErrorAction SilentlyContinue) {
        return @{
            Command = "py"
            Args = @("-3.11", "-m", "venv", $VenvPath)
            Label = "py -3.11"
        }
    }

    if (Get-Command python -ErrorAction SilentlyContinue) {
        $pythonVersion = (& python -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}')" 2>$null).Trim()
        if ($pythonVersion -and $pythonVersion -ne "3.11") {
            Write-Warning "Using Python $pythonVersion. The recommended stable version for this project is Python 3.11."
        }

        return @{
            Command = "python"
            Args = @("-m", "venv", $VenvPath)
            Label = "python"
        }
    }

    throw "Python was not found. Install 64-bit Python 3.11 first, then rerun this script."
}

if (-not (Test-Path $RequirementsPath)) {
    throw "requirements.txt was not found at $RequirementsPath"
}

if ($Recreate -and (Test-Path $VenvPath)) {
    Write-Host "Removing existing virtual environment at $VenvPath ..."
    Remove-Item -LiteralPath $VenvPath -Recurse -Force
}

if (-not (Test-Path $VenvPython)) {
    $launcher = Get-PythonLauncher
    Write-Host "Creating virtual environment with $($launcher.Label) ..."
    & $launcher.Command @($launcher.Args)
}
else {
    Write-Host "Using existing virtual environment at $VenvPath"
    $versionCheck = & $VenvPython --version 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Existing virtual environment is not usable: $versionCheck. Rerun with -Recreate to rebuild it."
    }
}

if (-not (Test-Path $VenvPython)) {
    throw "Virtual environment creation finished, but $VenvPython was not found."
}

if (-not $SkipInstall) {
    Write-Host "Upgrading pip tooling ..."
    & $VenvPython -m pip install --upgrade pip setuptools wheel

    Write-Host "Installing project packages from requirements.txt ..."
    & $VenvPython -m pip install -r $RequirementsPath
}

Write-Host ""
Write-Host "Environment ready."
Write-Host "Activate it with:"
Write-Host "  $ActivateScript"
Write-Host ""
Write-Host "Optional external dependency:"
Write-Host "  Install native Tesseract OCR separately if you plan to use pytesseract-based OCR helpers."
