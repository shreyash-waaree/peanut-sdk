# Finishes SDK install. sdkmanager needs JDK 11+ (auto-uses JDK 25/21/11 — not Java 8 from PATH).
# Run:  .\scripts\finish-android-sdk-setup.ps1
# Override:  $env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.2'; .\scripts\finish-android-sdk-setup.ps1

$ErrorActionPreference = "Stop"
$SdkRoot = "D:\Android"
$SdkManager = Join-Path $SdkRoot "cmdline-tools\latest\bin\sdkmanager.bat"

if (-not (Test-Path $SdkManager)) {
    Write-Error "Missing sdkmanager. Copy cmdline-tools into $SdkRoot\cmdline-tools\latest\ first."
}

function Test-IsJdk11Plus {
    param([string]$JdkRoot)
    $java = Join-Path $JdkRoot "bin\java.exe"
    if (-not (Test-Path $java)) { return $false }
    # java -version writes to stderr; avoid PowerShell treating it as a terminating stream
    $ver = cmd.exe /c "`"$java`" -version 2>&1"
    # Java 8 prints version "1.8.0_..." — sdkmanager needs 11+
    if ($ver -match 'version "1\.8') { return $false }
    if ($ver -match 'version "1\.[0-7]\.') { return $false }
    return $true
}

function Resolve-JdkHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe")) -and (Test-IsJdk11Plus $env:JAVA_HOME)) {
        return $env:JAVA_HOME
    }
    $searchRoots = @(
        "C:\Program Files\Java",
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Microsoft"
    )
    foreach ($root in $searchRoots) {
        if (-not (Test-Path $root)) { continue }
        $prefer = @("jdk-25*", "jdk-21*", "jdk-17*", "jdk-11*")
        foreach ($pat in $prefer) {
            $dirs = Get-ChildItem -Path $root -Directory -Filter $pat -ErrorAction SilentlyContinue
            foreach ($d in $dirs) {
                if (Test-Path (Join-Path $d.FullName "bin\java.exe")) {
                    return $d.FullName
                }
            }
        }
    }
    return $null
}

$jdkHome = Resolve-JdkHome
if (-not $jdkHome) {
    Write-Error "No JDK 11+ found. Set JAVA_HOME to e.g. C:\Program Files\Java\jdk-25.0.2"
}

$env:JAVA_HOME = $jdkHome
$env:Path = (Join-Path $jdkHome "bin") + ";" + $env:Path

Write-Host ('Using JAVA_HOME=' + $jdkHome)
Write-Host (cmd.exe /c "`"$jdkHome\bin\java.exe`" -version 2>&1")

Write-Host "Accepting licenses..."
$yes = "y`n" * 120
$yes | & $SdkManager --sdk_root=$SdkRoot --licenses

Write-Host "Installing platform-tools, API 29, build-tools 29.0.3..."
& $SdkManager --sdk_root=$SdkRoot 'platform-tools' 'platforms;android-29' 'build-tools;29.0.3'

Write-Host ('Done. SDK root: ' + $SdkRoot)
Write-Host 'Optional NDK (if Gradle asks):'
Write-Host ('  $env:JAVA_HOME=' + "'" + $jdkHome + "'" + '; & "' + $SdkManager + '" --sdk_root=' + $SdkRoot + ' "ndk;27.1.12297006"')
