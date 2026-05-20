# Free tray cameras for SampleApp debugging on Keenon / RockChip bots.
# Run from PowerShell before: adb connect <ip>:5555 ; then launch Count / camera tabs.
#
# Usage:
#   .\free-camera-for-sample.ps1
#   .\free-camera-for-sample.ps1 -Serial "10.245.157.167:5555"
#
# Stops other Keenon packages that often hold Camera1 / HAL. Edit $Packages if your image differs.
# Note: force-stopping peanutservice disables robot services until reboot or you start them again.

param(
    [string] $Serial = "10.245.157.167:5555"
)

$ErrorActionPreference = "Stop"

$Packages = @(
    "com.keenon.peanut.peanutservice",
    "com.keenon.peanut.solutionfooddelivery",
    "com.keenon.peanut.solutionsaler"
)

Write-Host "Connecting $Serial ..."
adb connect $Serial | Out-Host
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

foreach ($pkg in $Packages) {
    Write-Host "force-stop $pkg"
    adb -s $Serial shell "am force-stop $pkg" 2>&1 | Out-Host
}

Write-Host "`nCamera service (active clients should be empty before opening SampleApp):"
adb -s $Serial shell "dumpsys media.camera" 2>&1 |
    Select-String -Pattern "Active Camera|Number of camera|Camera module|Prior client|Device .* is" |
    ForEach-Object { $_.Line }

Write-Host "`nDone. Launch com.keenon.peanut.sample only, then Start preview in Count."
