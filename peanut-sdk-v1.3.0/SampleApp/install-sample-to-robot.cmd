@echo off
REM Install SampleApp debug APK to a specific robot via ADB.
REM Usage: install-sample-to-robot.cmd SERIAL
REM Example: install-sample-to-robot.cmd GFEFQ85Z1G
REM
REM Run from Command Prompt (cmd.exe), not PowerShell, if you see "65544-byte write failed".
REM Copy APK to C:\temp first (avoid OneDrive paths).

setlocal
set SERIAL=%~1
if "%SERIAL%"=="" (
  echo Usage: %~nx0 DEVICE_SERIAL
  echo Example: %~nx0 GFEFQ85Z1G
  echo.
  adb devices -l
  exit /b 1
)

set APK=C:\temp\app-debug-sample.apk
if not exist "%APK%" (
  echo ERROR: APK not found: %APK%
  echo Copy app-debug.apk from SampleApp\app\build\outputs\apk\debug\ to C:\temp\app-debug-sample.apk
  exit /b 1
)

echo Target: %SERIAL%
adb -s %SERIAL% get-state
if errorlevel 1 (
  echo ADB failed. Try: adb kill-server ^&^& adb start-server
  exit /b 1
)

set ADB_INSTALL_TIMEOUT=600000
echo Installing...
adb -s %SERIAL% install -r -d "%APK%"
if errorlevel 1 (
  echo.
  echo install failed — try two-step push:
  echo   adb -s %SERIAL% push "%APK%" /data/local/tmp/sample-debug.apk
  echo   adb -s %SERIAL% shell pm install -r -d /data/local/tmp/sample-debug.apk
  exit /b 1
)

echo Success.
exit /b 0
