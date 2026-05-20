# Capture camera logs from Keenon robot while you reproduce the issue on-device.
# Usage:  cd SampleApp\scripts   then   .\capture-camera-logs.ps1

$outDir = Join-Path $PSScriptRoot "..\camera-log-captures"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$logFile = Join-Path $outDir "camera-$stamp.txt"
$camFile = Join-Path $outDir "dumpsys-camera-$stamp.txt"

Write-Host "ADB devices:"
adb devices
if (-not (adb devices 2>&1 | Select-String "device$")) {
    Write-Host "ERROR: No device. Enable USB debugging and run adb devices"
    exit 1
}

adb shell dumpsys media.camera | Out-File -FilePath $camFile -Encoding utf8
adb logcat -c | Out-Null

Write-Host ""
Write-Host ">>> Reproduce on robot NOW (45 seconds) <<<"
Write-Host "    Developer -> Camera -> Start -> switch 0 to 1"
Write-Host ""

$proc = Start-Process -FilePath "adb" -ArgumentList @(
    "logcat", "-v", "time",
    "CameraFragment:E", "CountFragment:E", "YoloFragment:E",
    "Camera:W", "cameraserver:E", "mediaserver:E"
) -RedirectStandardOutput $logFile -NoNewWindow -PassThru

Start-Sleep -Seconds 45
Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue

Write-Host "Saved:"
Write-Host "  $logFile"
Write-Host "  $camFile"
Write-Host ""
Select-String -Path $logFile -Pattern "failed|error|died|empty|HAL|getParameters|CameraFragment|CountFragment|YoloFragment" -CaseSensitive:$false | Select-Object -First 40
