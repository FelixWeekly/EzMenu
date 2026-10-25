param([string]$JavaHome = $env:JAVA_HOME, [switch]$Device)
$ErrorActionPreference = 'Stop'
if (-not $JavaHome -or -not (Test-Path (Join-Path $JavaHome 'bin/java.exe'))) {
    throw 'Set JAVA_HOME to a supported JDK (the tested version is JDK 21), or pass -JavaHome.'
}
$env:JAVA_HOME = $JavaHome
$taskRoot = Split-Path -Parent $PSScriptRoot
Push-Location $taskRoot
try {
    $tasks = @(':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug', ':app:assembleRelease')
    if ($Device) { $tasks += @(':app:connectedDebugAndroidTest', '-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true') }
    & .\gradlew.bat @tasks --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Verification failed; inspect the Gradle reports.' }
} finally { Pop-Location }
