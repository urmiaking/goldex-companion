[CmdletBinding()]
param(
    [ValidateSet("core","windows","android","shared","all")]
    [string]$Target = "all",
    [switch]$Ui
)
$ErrorActionPreference = "Stop"
$repo = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
Push-Location $repo
try {
    $gradleTasks = switch ($Target) {
        "core"    { @(":core:verifyCoreBoundaries", ":core:jvmTest") }
        "windows" { @(":core:jvmTest", ":desktop:test") }
        "android" { @(":core:jvmTest", ":app:testDebugUnitTest") }
        "shared"  { @(":core:verifyCoreBoundaries", ":core:jvmTest", ":desktop:test", ":app:testDebugUnitTest") }
        "all"     { @(":core:verifyCoreBoundaries", ":core:jvmTest", ":desktop:test", ":app:testDebugUnitTest") }
    }
    if ($Ui) {
        if ($Target -in @("android","core")) {
            throw "-Ui is for desktop Compose UI tests; select -Target windows/shared/all."
        }
        $gradleTasks += ":desktop:uiTest"
    }
    Write-Host "Qirato fast verification: $Target (Compose UI: $Ui)"
    & ./gradlew.bat @gradleTasks --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle verification failed (exit $LASTEXITCODE)" }
} finally {
    Pop-Location
}
