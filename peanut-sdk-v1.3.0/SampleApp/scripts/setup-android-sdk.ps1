# Run in PowerShell AFTER JDK 17+ is installed (e.g. winget install Microsoft.OpenJDK.17 - approve UAC).
# Installs API 29 + build-tools for SampleApp into D:\Android

$ErrorActionPreference = "Stop"
$SdkRoot = "D:\Android"
$SdkManager = Join-Path $SdkRoot "cmdline-tools\latest\bin\sdkmanager.bat"

if (-not (Test-Path $SdkManager)) {
    Write-Error "Not found: $SdkManager — copy cmdline-tools into $SdkRoot\cmdline-tools\latest\"
}

$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
    Write-Error "java not in PATH. Install JDK 17+ and reopen the terminal."
}

& java -version 2>&1 | Write-Host

$env:SKIP_JDK_VERSION_CHECK = "1"
$yes = "y`n" * 120

Write-Host "Accepting licenses..."
$yes | & $SdkManager --sdk_root=$SdkRoot --licenses

Write-Host "Installing platform-tools, android-29, build-tools 29.0.3..."
& $SdkManager --sdk_root=$SdkRoot "platform-tools" "platforms;android-29" "build-tools;29.0.3"

Write-Host "Done. SDK root: $SdkRoot"
