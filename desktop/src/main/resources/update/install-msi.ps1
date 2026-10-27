param([string]$Plan, [switch]$ValidateOnly, [switch]$CleanupOnly, [string]$InstalledVersion, [string]$DataDirectory)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
function FullPath([string]$Value) { [IO.Path]::GetFullPath($Value).TrimEnd('\') }
function PlainPath([string]$Value) {
    $current = $Value
    while ($current) {
        $item = Get-Item -LiteralPath $current -Force
        if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Reparse point in installation' }
        $current = Split-Path $current -Parent
    }
}
function WaitForDataLock([string]$Path, [int]$TimeoutMilliseconds = 30000) {
    # A native launcher can exit before its JVM releases the store. Never force
    # another process to close or install while financial storage is still open.
    $deadline = [DateTime]::UtcNow.AddMilliseconds($TimeoutMilliseconds)
    do {
        try { return [IO.File]::Open($Path, [IO.FileMode]::Open, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None) }
        catch [IO.IOException] {
            if ([DateTime]::UtcNow -ge $deadline) { throw 'Data is still in use; update postponed without installation' }
            Start-Sleep -Milliseconds 100
        }
    } while ($true)
}
function RemovePreviousUpdateStages([string]$CurrentStage, [string]$InstallRoot, [string]$Version, [string]$DataDirectory) {
    # Keep the current MSI at its registered source location for Windows repair.
    # Delete only flat, recognized, successful older helper directories. Never
    # recurse into Programs, application files, user storage or reparse points.
    $installPath = FullPath $InstallRoot
    $currentPath = FullPath $CurrentStage
    $programs = FullPath (Join-Path $env:LOCALAPPDATA 'Programs')
    $storage = FullPath $DataDirectory
    if ($installPath -ine (Join-Path $programs 'Qirato') -or
        $storage -ieq $programs -or $storage.StartsWith($programs + '\', [StringComparison]::OrdinalIgnoreCase) -or
        (Split-Path $currentPath -Parent) -ine $programs -or
        (Split-Path $currentPath -Leaf) -notmatch '^\.qirato-update-[0-9a-f-]{36}$' -or
        $Version -notmatch '^\d+\.\d+\.\d+$') { return }
    PlainPath $installPath; PlainPath $currentPath
    $success = Get-Content -LiteralPath (Join-Path $currentPath 'result.json') -Raw | ConvertFrom-Json
    if ($success.success -isnot [bool] -or !$success.success -or $success.version -ne $Version) { return }
    foreach ($candidate in @(Get-ChildItem -LiteralPath $programs -Directory -Force)) {
        try {
            $path = FullPath $candidate.FullName
            if ($path -ieq $currentPath -or $candidate.Name -notmatch '^\.qirato-update-[0-9a-f-]{36}$' -or
                (Split-Path $path -Parent) -ine $programs) { continue }
            PlainPath $path
            $entries = @(Get-ChildItem -LiteralPath $path -Force)
            if (@($entries | Where-Object { $_.PSIsContainer -or ($_.Attributes -band [IO.FileAttributes]::ReparsePoint) }).Count -gt 0) { continue }
            $oldPlanFile = Join-Path $path 'plan.json'
            $oldResultFile = Join-Path $path 'result.json'
            if (!(Test-Path -LiteralPath $oldPlanFile -PathType Leaf) -or !(Test-Path -LiteralPath $oldResultFile -PathType Leaf) -or
                (Get-Item -LiteralPath $oldPlanFile).Length -gt 65536 -or (Get-Item -LiteralPath $oldResultFile).Length -gt 65536) { continue }
            $oldPlan = Get-Content -LiteralPath $oldPlanFile -Raw | ConvertFrom-Json
            $oldResult = Get-Content -LiteralPath $oldResultFile -Raw | ConvertFrom-Json
            if ($oldResult.success -isnot [bool] -or !$oldResult.success -or
                $oldPlan.version -notmatch '^\d+\.\d+\.\d+$' -or $oldResult.version -ne $oldPlan.version -or
                [version]$oldPlan.version -ge [version]$Version -or
                (FullPath $oldPlan.installRoot) -ine $installPath -or (FullPath $oldPlan.stagingRoot) -ine $path -or
                (FullPath $oldPlan.dataDirectory) -ine (FullPath $DataDirectory)) { continue }
            $msiName = "Qirato-Windows-x64-$($oldPlan.version).msi"
            if ((FullPath $oldPlan.bundle) -ine (Join-Path $path $msiName)) { continue }
            $allowed = @('plan.json','result.json','install.ps1','ready','runtime.log','helper.log','msi-verify.log','helper-out.log','helper-error.log',$msiName)
            if (@($entries | Where-Object { $_.Name -notin $allowed }).Count -gt 0) { continue }
            # Absolute direct children have now been checked. No recursive delete.
            foreach ($entry in $entries) { Remove-Item -LiteralPath $entry.FullName -Force }
            Remove-Item -LiteralPath $path -Force
        } catch {
            # Locked or unfamiliar caches stay intact; cleanup cannot fail an upgrade.
        }
    }
}
function AssertInstaller {
    PlainPath $root; PlainPath $stage; PlainPath $package
    # Start-Process from pwsh can inherit a PSModulePath that omits Windows
    # PowerShell's Get-FileHash module. Use the runtime's .NET implementation.
    $sha = [Security.Cryptography.SHA256]::Create()
    $stream = [IO.File]::OpenRead($package)
    try { $digest = [BitConverter]::ToString($sha.ComputeHash($stream)).Replace('-','').ToLowerInvariant() }
    finally { $stream.Dispose(); $sha.Dispose() }
    if ((Get-Item -LiteralPath $package).Length -ne [long]$planData.size -or
        $digest -ine $planData.sha256) { throw 'Installer checksum mismatch' }
    $installer = New-Object -ComObject WindowsInstaller.Installer
    $database = $null
    try {
        $database = $installer.OpenDatabase($package, 0)
        foreach ($pair in @(
            @('ProductName','Qirato'), @('Manufacturer','Qirato'), @('ProductVersion',[string]$planData.version),
            @('UpgradeCode','{A6B3CF2A-A8F7-4CDE-A49A-F8935C6B8306}'), @('QIRATO_INSTALLER','1'))) {
            $view = $database.OpenView("SELECT Value FROM Property WHERE Property = '$($pair[0])'")
            $record = $null
            try {
                $view.Execute(); $record = $view.Fetch()
                if (!$record -or $record.StringData(1) -ine $pair[1]) { throw 'Unexpected installer identity' }
            } finally {
                if ($record) { [void][Runtime.InteropServices.Marshal]::ReleaseComObject($record) }
                $view.Close(); [void][Runtime.InteropServices.Marshal]::ReleaseComObject($view)
            }
        }
    } finally {
        if ($database) { [void][Runtime.InteropServices.Marshal]::ReleaseComObject($database) }
        [void][Runtime.InteropServices.Marshal]::ReleaseComObject($installer)
    }
}
if ($CleanupOnly) {
    # Also recover caches left by older helper versions. The new application
    # supplies its own running version; a successful matching plan is required.
    $installedRoot = FullPath (Join-Path $env:LOCALAPPDATA 'Programs\Qirato')
    if ($InstalledVersion -notmatch '^\d+\.\d+\.\d+$' -or !$DataDirectory -or
        !(Test-Path -LiteralPath (Join-Path $installedRoot '.qirato-msi') -PathType Leaf)) { exit 0 }
    PlainPath $installedRoot
    foreach ($cache in @(Get-ChildItem -LiteralPath (Split-Path $installedRoot -Parent) -Directory -Force)) {
        try {
            if ($cache.Name -notmatch '^\.qirato-update-[0-9a-f-]{36}$') { continue }
            PlainPath $cache.FullName
            $cachePlan = Join-Path $cache.FullName 'plan.json'
            $cacheResult = Join-Path $cache.FullName 'result.json'
            if (!(Test-Path -LiteralPath $cachePlan -PathType Leaf) -or !(Test-Path -LiteralPath $cacheResult -PathType Leaf)) { continue }
            PlainPath $cachePlan; PlainPath $cacheResult
            if ((Get-Item -LiteralPath $cachePlan).Length -gt 65536 -or (Get-Item -LiteralPath $cacheResult).Length -gt 65536) { continue }
            $metadata = Get-Content -LiteralPath $cachePlan -Raw | ConvertFrom-Json
            if ($metadata.version -ne $InstalledVersion -or (FullPath $metadata.installRoot) -ine $installedRoot -or
                (FullPath $metadata.stagingRoot) -ine (FullPath $cache.FullName) -or
                (FullPath $metadata.dataDirectory) -ine (FullPath $DataDirectory)) { continue }
            RemovePreviousUpdateStages $cache.FullName $installedRoot $InstalledVersion $DataDirectory
        } catch { }
    }
    exit 0
}
$planData = Get-Content -LiteralPath $Plan -Raw | ConvertFrom-Json
$root = FullPath $planData.installRoot
$stage = FullPath $planData.stagingRoot
$package = FullPath $planData.bundle
$expectedRoot = FullPath (Join-Path $env:LOCALAPPDATA 'Programs\Qirato')
$data = FullPath $planData.dataDirectory
if ($root -ine $expectedRoot -or (Split-Path $stage -Parent) -ine (Split-Path $root -Parent) -or
    (Split-Path $stage -Leaf) -notmatch '^\.qirato-update-[0-9a-f-]{36}$' -or
    $package -ine (Join-Path $stage "Qirato-Windows-x64-$($planData.version).msi") -or (FullPath $Plan) -ine (Join-Path $stage 'plan.json') -or
    (FullPath $PSScriptRoot) -ine $stage -or $planData.version -notmatch '^\d+\.\d+\.\d+$' -or
    $planData.sha256 -notmatch '^[a-f0-9]{64}$' -or [long]$planData.size -lt 1 -or [long]$planData.size -gt 300MB -or
    [long]$planData.processId -le 0 -or [long]$planData.processId -eq $PID -or
    $data -ieq $root -or $data.StartsWith($root + '\', [StringComparison]::OrdinalIgnoreCase) -or
    !(Test-Path -LiteralPath (Join-Path $root '.qirato-msi') -PathType Leaf)) { throw 'Invalid installed update plan' }
AssertInstaller
if ($ValidateOnly) { exit 0 }
$running = Get-Process -Id ([int]$planData.processId) -ErrorAction Stop
if ((FullPath $running.Path) -ine (Join-Path $root 'Qirato.exe')) { throw 'Running process does not match installation' }
[IO.File]::WriteAllText((Join-Path $stage 'ready'), 'ready')
$exited = $false
try {
    if (!$running.WaitForExit(120000)) { throw 'Application did not exit; update postponed' }
    $exited = $true
    $lock = WaitForDataLock (Join-Path $data 'workspace.lock')
    try {
        # Revalidate immediately before executing. Windows Installer owns the
        # transactional removal/replacement of the previous registered version.
        AssertInstaller
        $msiexec = Join-Path $env:SystemRoot 'System32\msiexec.exe'
        $install = Start-Process $msiexec -ArgumentList @('/i', ('"' + $package + '"'), '/qn', '/norestart', 'REBOOT=ReallySuppress') -WindowStyle Hidden -PassThru -Wait
        if ($install.ExitCode -notin @(0,3010)) { throw "Windows Installer could not upgrade the application (code=$($install.ExitCode))" }
        $log = Join-Path $stage 'runtime.log'
        $verification = Start-Process (Join-Path $root 'Qirato.exe') -ArgumentList '--verify-runtime' -RedirectStandardOutput $log -WindowStyle Hidden -PassThru
        [void]$verification.Handle # Keep the process handle so Windows PowerShell retains ExitCode.
        if (!$verification.WaitForExit(60000)) { $verification.Kill(); throw 'Installed runtime verification timed out' }
        $verification.WaitForExit() # Drain redirected output before reading the version marker.
        if ($verification.ExitCode -ne 0 -or (Get-Item -LiteralPath $log).Length -gt 65536 -or
            [IO.File]::ReadAllLines($log) -notcontains "Qirato runtime version=$($planData.version)") { throw 'Installed runtime verification failed' }
    } finally { $lock.Dispose() }
    Start-Process (Join-Path $root 'Qirato.exe') -WorkingDirectory $root -WindowStyle Normal
    @{success=$true; version=$planData.version} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $stage 'result.json') -Encoding UTF8
    try { RemovePreviousUpdateStages $stage $root $planData.version $data } catch { }
} catch {
    # This helper handles only installer/process metadata, never financial JSON.
    [Console]::Error.WriteLine($_.Exception.Message)
    # MSI failure rolls back in its own transaction. Reopen whichever registered
    # runtime remains, without deleting data or forcing a running app to close.
    if ($exited -and (Test-Path -LiteralPath (Join-Path $root 'Qirato.exe'))) {
        Start-Process (Join-Path $root 'Qirato.exe') -WorkingDirectory $root -WindowStyle Normal
    }
    @{success=$false; version=$planData.version} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $stage 'result.json') -Encoding UTF8
    exit 1
}
