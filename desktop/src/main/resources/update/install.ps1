param([Parameter(Mandatory=$true)][string]$Plan)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

# This helper is outside the running bundle. All moves remain among checked
# siblings; it never removes directories or touches the business data files.
function FullPath([string]$Value) { [IO.Path]::GetFullPath($Value).TrimEnd('\') }
function PlainDirectory([string]$Value) {
    $item = Get-Item -LiteralPath $Value
    if (!$item.PSIsContainer -or ($item.Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw 'Unsupported directory' }
}
function MoveDirectory([string]$Source, [string]$Destination) {
    # Directory.Move refuses an existing destination rather than nesting/merging.
    for ($attempt = 0; $attempt -lt 20; $attempt++) {
        if (Test-Path -LiteralPath $Destination) { throw 'Destination already exists' }
        try { [IO.Directory]::Move($Source, $Destination); return }
        catch { if ($attempt -eq 19) { throw }; Start-Sleep -Milliseconds 250 }
    }
}
function LaunchApp([string]$Root) {
    $launched = Start-Process -FilePath (Join-Path $Root 'Qirato.exe') -WorkingDirectory $Root -WindowStyle Normal -PassThru
    Start-Sleep -Seconds 3
    if ($launched.HasExited) { throw 'Application exited during startup' }
}

$ready = $null
$oldMoved = $false
$newMoved = $false
$planData = Get-Content -LiteralPath $Plan -Raw -Encoding UTF8 | ConvertFrom-Json
$root = FullPath $planData.installRoot
$stage = FullPath $planData.stagingRoot
$bundle = FullPath $planData.bundle
$parent = Split-Path $root -Parent
if ((Split-Path $root -Leaf) -ine 'Qirato' -or !$parent -or (Split-Path $stage -Parent) -ine $parent -or
    (Split-Path $stage -Leaf) -notmatch '^\.qirato-update-[0-9a-f-]{36}$' -or
    $bundle -ine (Join-Path $stage 'extracted\Qirato') -or (FullPath $Plan) -ine (Join-Path $stage 'plan.json') -or
    (FullPath $PSScriptRoot) -ine $stage -or $planData.version -notmatch '^\d+\.\d+\.\d+$' -or
    [long]$planData.processId -le 0 -or [long]$planData.processId -eq $PID) { throw 'Invalid update plan' }
$data = FullPath $planData.dataDirectory
if ($data -ieq $root -or $data.StartsWith($root + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Data inside installation' }
PlainDirectory $parent; PlainDirectory $root; PlainDirectory $stage; PlainDirectory $bundle
foreach ($directory in Get-ChildItem -LiteralPath $bundle -Directory -Recurse) {
    if ($directory.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Reparse point in update' }
}
if (!(Test-Path -LiteralPath (Join-Path $bundle 'Qirato.exe') -PathType Leaf) -or
    !(Test-Path -LiteralPath (Join-Path $bundle 'app\Qirato.cfg') -PathType Leaf) -or
    !(Test-Path -LiteralPath (Join-Path $bundle 'runtime') -PathType Container)) { throw 'Incomplete update' }
$suffix = (Split-Path $stage -Leaf).Substring('.qirato-update-'.Length)
$backup = Join-Path $parent ('.qirato-previous-' + $suffix)
$failed = Join-Path $parent ('.qirato-failed-' + $suffix)
if ((Test-Path -LiteralPath $backup) -or (Test-Path -LiteralPath $failed)) { throw 'Backup path already exists' }
$result = Join-Path $stage 'result.json'
try {
    $running = Get-Process -Id ([int]$planData.processId) -ErrorAction SilentlyContinue
    if ($running -and (FullPath $running.Path) -ine (Join-Path $root 'Qirato.exe')) { throw 'Running process does not match installation' }
    $ready = Join-Path $stage 'ready'
    [IO.File]::WriteAllText($ready, 'ready')
    if ($running -and !$running.WaitForExit(120000)) { throw 'Application did not exit; update postponed' }
    # A second Qirato window must not acquire the data lock during the swap.
    $lock = [IO.File]::Open((Join-Path $data 'workspace.lock'), [IO.FileMode]::Open, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None)
    try {
        PlainDirectory $root; PlainDirectory $bundle
        MoveDirectory $root $backup
        $oldMoved = $true
        MoveDirectory $bundle $root
        $newMoved = $true
    } finally { $lock.Dispose() }
    LaunchApp $root
    try { @{ success=$true; version=$planData.version; backup=$backup } | ConvertTo-Json | Set-Content -LiteralPath $result -Encoding UTF8 } catch { }
} catch {
    # No killing of the user's app and no recursive removal on a failed update.
    if ($oldMoved) {
        if ($newMoved -and (Test-Path -LiteralPath $root)) { MoveDirectory $root $failed }
        MoveDirectory $backup $root
        try { LaunchApp $root } catch { }
    } elseif ($running -and $running.HasExited -and (Test-Path -LiteralPath $root)) {
        try { LaunchApp $root } catch { }
    }
    @{ success=$false; version=$planData.version; previousRestored=$oldMoved } | ConvertTo-Json | Set-Content -LiteralPath $result -Encoding UTF8
    exit 1
}
