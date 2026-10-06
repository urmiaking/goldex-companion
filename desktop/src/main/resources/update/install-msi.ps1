param([Parameter(Mandatory=$true)][string]$Plan, [switch]$ValidateOnly)
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
$planData = Get-Content -LiteralPath $Plan -Raw | ConvertFrom-Json
$root = FullPath $planData.installRoot
$stage = FullPath $planData.stagingRoot
$package = FullPath $planData.bundle
$expectedRoot = FullPath (Join-Path $env:LOCALAPPDATA 'Programs\Qirato')
$data = FullPath $planData.dataDirectory
if ($root -ine $expectedRoot -or (Split-Path $stage -Parent) -ine (Split-Path $root -Parent) -or
    (Split-Path $stage -Leaf) -notmatch '^\.qirato-update-[0-9a-f-]{36}$' -or
    $package -ine (Join-Path $stage 'package.msi') -or (FullPath $Plan) -ine (Join-Path $stage 'plan.json') -or
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
    $lock = [IO.File]::Open((Join-Path $data 'workspace.lock'), [IO.FileMode]::Open, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None)
    try {
        # Revalidate immediately before executing. Windows Installer owns the
        # transactional removal/replacement of the previous registered version.
        AssertInstaller
        $msiexec = Join-Path $env:SystemRoot 'System32\msiexec.exe'
        $install = Start-Process $msiexec -ArgumentList @('/i', ('"' + $package + '"'), '/qn', '/norestart', 'REBOOT=ReallySuppress') -WindowStyle Hidden -PassThru -Wait
        if ($install.ExitCode -notin @(0,3010)) { throw 'Windows Installer could not upgrade the application' }
        $log = Join-Path $stage 'runtime.log'
        $verification = Start-Process (Join-Path $root 'Qirato.exe') -ArgumentList '--verify-runtime' -RedirectStandardOutput $log -WindowStyle Hidden -PassThru
        if (!$verification.WaitForExit(60000)) { $verification.Kill(); throw 'Installed runtime verification timed out' }
        $verification.WaitForExit() # Drain redirected output before reading the version marker.
        if ($verification.ExitCode -ne 0 -or (Get-Item -LiteralPath $log).Length -gt 65536 -or
            [IO.File]::ReadAllLines($log) -notcontains "Qirato runtime version=$($planData.version)") { throw 'Installed runtime verification failed' }
    } finally { $lock.Dispose() }
    Start-Process (Join-Path $root 'Qirato.exe') -WorkingDirectory $root -WindowStyle Normal
    @{success=$true; version=$planData.version} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $stage 'result.json') -Encoding UTF8
} catch {
    # MSI failure rolls back in its own transaction. Reopen whichever registered
    # runtime remains, without deleting data or forcing a running app to close.
    if ($exited -and (Test-Path -LiteralPath (Join-Path $root 'Qirato.exe'))) {
        Start-Process (Join-Path $root 'Qirato.exe') -WorkingDirectory $root -WindowStyle Normal
    }
    @{success=$false; version=$planData.version} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $stage 'result.json') -Encoding UTF8
    exit 1
}
