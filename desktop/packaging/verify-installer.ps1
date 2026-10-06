param([Parameter(Mandatory=$true)][string]$Version)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
# Installation mutates the current user's product registration. Run only on the
# disposable GitHub Windows runner, never against a developer's installed app.
if ($env:GITHUB_ACTIONS -ne 'true' -or !$env:RUNNER_TEMP) { throw 'Installer integration requires a disposable GitHub runner' }
$root = Join-Path $env:LOCALAPPDATA 'Programs\Qirato'
$data = Join-Path $env:LOCALAPPDATA 'Qirato\Desktop'
if ((Test-Path -LiteralPath $root) -or (Test-Path -LiteralPath $data)) { throw 'Runner contains pre-existing application or data' }
$output = [IO.Path]::GetFullPath("$PSScriptRoot\..\build\installer")
$image = [IO.Path]::GetFullPath("$PSScriptRoot\..\build\compose\binaries\main\app\Qirato")
$parts = $Version.Split('.')
if ([int]$parts[2] -lt 1) { throw 'Test fixture needs a lower patch version' }
$previousVersion = "$($parts[0]).$($parts[1]).$([int]$parts[2] - 1)"
$previousOutput = Join-Path $env:RUNNER_TEMP 'qirato-previous-installer'
$previousImage = Join-Path $env:RUNNER_TEMP 'qirato-previous-image'
Copy-Item -LiteralPath $image -Destination $previousImage -Recurse
[IO.File]::WriteAllText((Join-Path $previousImage 'app\previous-only.txt'), 'obsolete installer-owned file')
# Previous product metadata over this runtime tests installer ownership,
# upgrade/removal and the actual shipped helper without relying on old assets.
& "$PSScriptRoot\build-installer.ps1" -AppImage $previousImage -Version $previousVersion -Destination $previousOutput
$previous = Join-Path $previousOutput "Qirato-Windows-x64-$previousVersion.msi"
$current = Join-Path $output "Qirato-Windows-x64-$Version.msi"
$msiexec = Join-Path $env:SystemRoot 'System32\msiexec.exe'
function Install([string]$Package) {
    $result = Start-Process $msiexec -ArgumentList @('/i', ('"'+$Package+'"'), '/qn', '/norestart') -WindowStyle Hidden -PassThru -Wait
    if ($result.ExitCode -ne 0) { throw "Installer failed ($($result.ExitCode))" }
}
function Products {
    $installer = New-Object -ComObject WindowsInstaller.Installer
    $related = $null
    try {
        # MSI automation properties have incomplete type information in pwsh.
        # Use explicit IDispatch property access rather than dynamic adaptation.
        $related = $installer.GetType().InvokeMember('RelatedProducts', [Reflection.BindingFlags]::GetProperty, $null, $installer,
            @('{A6B3CF2A-A8F7-4CDE-A49A-F8935C6B8306}'))
        $count = $related.GetType().InvokeMember('Count', [Reflection.BindingFlags]::GetProperty, $null, $related, $null)
        for ($index = 0; $index -lt $count; $index++) {
            $code = $related.GetType().InvokeMember('Item', [Reflection.BindingFlags]::GetProperty, $null, $related, @($index))
            [pscustomobject]@{PSChildName=[string]$code;
                DisplayVersion=$installer.GetType().InvokeMember('ProductInfo', [Reflection.BindingFlags]::GetProperty, $null, $installer, @($code,'VersionString'));
                AssignmentType=$installer.GetType().InvokeMember('ProductInfo', [Reflection.BindingFlags]::GetProperty, $null, $installer, @($code,'AssignmentType'))}
        }
    } finally {
        if ($related) { [void][Runtime.InteropServices.Marshal]::ReleaseComObject($related) }
        [void][Runtime.InteropServices.Marshal]::ReleaseComObject($installer)
    }
}
function AssertShortcuts {
    $shell = New-Object -ComObject WScript.Shell
    try {
        foreach ($shortcut in @((Join-Path ([Environment]::GetFolderPath('Desktop')) 'Qirato.lnk'),
            (Join-Path ([Environment]::GetFolderPath('Programs')) 'Qirato\Qirato.lnk'))) {
            if (!(Test-Path -LiteralPath $shortcut) -or $shell.CreateShortcut($shortcut).TargetPath -ine (Join-Path $root 'Qirato.exe')) { throw 'Installer shortcut missing or wrong' }
        }
    } finally { [void][Runtime.InteropServices.Marshal]::ReleaseComObject($shell) }
}
Install $previous
$old = @(Products)
if ($old.Count -ne 1 -or $old[0].DisplayVersion -ne $previousVersion -or $old[0].AssignmentType -ne '0') {
    $old | Format-Table PSChildName,DisplayVersion,AssignmentType
    throw 'Previous per-user fixture did not register'
}
AssertShortcuts
New-Item -ItemType Directory -Path $data -Force | Out-Null
# This sentinel is outside the install tree and is never parsed as business data.
$sentinel = Join-Path $data 'installer-test-sentinel.txt'
[IO.File]::WriteAllText($sentinel, 'preserved across install upgrade uninstall')
$before = (Get-FileHash -LiteralPath $sentinel).Hash
$app = Start-Process (Join-Path $root 'Qirato.exe') -WorkingDirectory $root -WindowStyle Hidden -PassThru
$deadline = [DateTime]::UtcNow.AddSeconds(30)
while (!(Test-Path -LiteralPath (Join-Path $data 'workspace.lock')) -and !$app.HasExited -and [DateTime]::UtcNow -lt $deadline) { Start-Sleep -Milliseconds 100 }
if (!(Test-Path -LiteralPath (Join-Path $data 'workspace.lock')) -or $app.HasExited) { throw 'Installed application did not open its data store' }
$stage = Join-Path (Split-Path $root -Parent) ('.qirato-update-' + [guid]::NewGuid().ToString())
New-Item -ItemType Directory -Path $stage | Out-Null
$package = Join-Path $stage 'package.msi'
Copy-Item -LiteralPath $current -Destination $package
Copy-Item -LiteralPath "$PSScriptRoot\..\src\main\resources\update\install-msi.ps1" -Destination (Join-Path $stage 'install.ps1')
$plan = Join-Path $stage 'plan.json'
@{installRoot=$root;stagingRoot=$stage;bundle=$package;processId=$app.Id;version=$Version;dataDirectory=$data;
    sha256=(Get-FileHash -LiteralPath $package).Hash.ToLowerInvariant();size=(Get-Item -LiteralPath $package).Length} |
    ConvertTo-Json | Set-Content -LiteralPath $plan -Encoding UTF8
$shellPath = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
$validPlan = Get-Content -LiteralPath $plan -Raw
$corruptPlan = $validPlan | ConvertFrom-Json
$corruptPlan.sha256 = '0' * 64
$corruptPlan | ConvertTo-Json | Set-Content -LiteralPath $plan -Encoding UTF8
$rejected = Start-Process $shellPath -ArgumentList @('-NoProfile','-NonInteractive','-File',
    ('"'+(Join-Path $stage 'install.ps1')+'"'), '-Plan', ('"'+$plan+'"'), '-ValidateOnly') -WindowStyle Hidden -PassThru -Wait
if ($rejected.ExitCode -eq 0 -or (Test-Path -LiteralPath (Join-Path $stage 'ready')) -or $app.HasExited) { throw 'Corrupt update was not rejected before restart' }
$validPlan | Set-Content -LiteralPath $plan -Encoding UTF8
$helper = Start-Process $shellPath -ArgumentList @('-NoProfile','-NonInteractive','-WindowStyle','Hidden','-ExecutionPolicy','Bypass',
    '-File', ('"'+(Join-Path $stage 'install.ps1')+'"'), '-Plan', ('"'+$plan+'"')) -WindowStyle Hidden -PassThru
$deadline = [DateTime]::UtcNow.AddSeconds(30)
while (!(Test-Path -LiteralPath (Join-Path $stage 'ready')) -and !$helper.HasExited -and [DateTime]::UtcNow -lt $deadline) { Start-Sleep -Milliseconds 100 }
if (!(Test-Path -LiteralPath (Join-Path $stage 'ready')) -or $app.HasExited) { throw 'Helper did not acknowledge while app remained open' }
# Only the disposable runner's explicitly launched fixture process is stopped.
Stop-Process -Id $app.Id -Force
if (!$helper.WaitForExit(180000) -or $helper.ExitCode -ne 0) { throw 'Shipped updater helper failed' }
$installed = @(Products)
if ($installed.Count -ne 1 -or $installed[0].DisplayVersion -ne $Version -or $installed[0].PSChildName -eq $old[0].PSChildName) { throw 'Previous product was not replaced' }
if (Test-Path -LiteralPath (Join-Path $root 'app\previous-only.txt')) { throw 'Obsolete installer-owned file remained after upgrade' }
if ((Get-FileHash -LiteralPath $sentinel).Hash -ne $before) { throw 'Upgrade changed user data' }
AssertShortcuts
$reopened = @(Get-Process Qirato -ErrorAction SilentlyContinue | Where-Object { $_.Path -ieq (Join-Path $root 'Qirato.exe') })
if ($reopened.Count -ne 1) { throw 'Updater did not reopen the installed application' }
$reopened | Stop-Process -Force
# Reinstall is idempotent and older packages are rejected.
Install $current
$downgrade = Start-Process $msiexec -ArgumentList @('/i', ('"'+$previous+'"'), '/qn', '/norestart') -WindowStyle Hidden -PassThru -Wait
if ($downgrade.ExitCode -eq 0) { throw 'Downgrade was allowed' }
$uninstall = Start-Process $msiexec -ArgumentList @('/x', $installed[0].PSChildName, '/qn', '/norestart') -WindowStyle Hidden -PassThru -Wait
if ($uninstall.ExitCode -ne 0 -or @(Products).Count -ne 0 -or (Test-Path -LiteralPath (Join-Path $root 'Qirato.exe'))) { throw 'Uninstall did not remove owned application' }
if ((Get-FileHash -LiteralPath $sentinel).Hash -ne $before) { throw 'Uninstall changed user data' }
Write-Output 'Installer verified: shortcuts, helper wait/reopen, major upgrade, downgrade rejection, uninstall, data preservation.'
