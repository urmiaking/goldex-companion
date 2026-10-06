param(
    [string]$AppImage = "$PSScriptRoot\..\build\compose\binaries\main\app\Qirato",
    [string]$Version = ((Get-Content "$PSScriptRoot\..\version.properties" | Where-Object { $_ -match '^version=' }) -replace '^version=', ''),
    [string]$Destination = "$PSScriptRoot\..\build\installer"
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
if ($Version -notmatch '^\d+\.\d+\.\d+$') { throw 'Invalid installer version' }
$AppImage = (Resolve-Path -LiteralPath $AppImage).Path
if (!(Test-Path -LiteralPath (Join-Path $AppImage 'Qirato.exe'))) { throw 'Build the application image first' }
$tools = [IO.Path]::GetFullPath("$PSScriptRoot\..\build\installer-tools")
New-Item -ItemType Directory -Path $tools, $Destination -Force | Out-Null
$zip = Join-Path $tools 'wix314-binaries.zip'
$checksum = '6ac824e1642d6f7277d0ed7ea09411a508f6116ba6fae0aa5f2c7daa2ff43d31'
if (!(Test-Path -LiteralPath $zip)) {
    Invoke-WebRequest 'https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip' -OutFile $zip
}
if ((Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash -ine $checksum) { throw 'WiX download verification failed' }
if (!(Test-Path -LiteralPath (Join-Path $tools 'wix314\candle.exe'))) { Expand-Archive -LiteralPath $zip -DestinationPath (Join-Path $tools 'wix314') }
$wix = Join-Path $tools 'wix314'
$build = Join-Path ([IO.Path]::GetFullPath($Destination)) ([guid]::NewGuid().ToString())
New-Item -ItemType Directory -Path $build | Out-Null
$marker = Join-Path $build '.qirato-msi'
[IO.File]::WriteAllText($marker, 'Qirato per-user Windows Installer')
$files = Join-Path $build 'files.wxs'
& "$wix\heat.exe" dir $AppImage -nologo -gg -sfrag -srd -sreg -dr INSTALLFOLDER -cg AppFiles -var var.AppImage -out $files
if ($LASTEXITCODE -ne 0) { throw 'Installer file harvesting failed' }
# Per-user file components need HKCU key paths and uninstallable directories.
# Author those in the generated fragment instead of suppressing ICE38/ICE64.
[xml]$harvest = Get-Content -LiteralPath $files -Raw
$ns = New-Object Xml.XmlNamespaceManager($harvest.NameTable)
$ns.AddNamespace('w', 'http://schemas.microsoft.com/wix/2006/wi')
foreach ($component in $harvest.SelectNodes('//w:Component', $ns)) {
    $component.SetAttribute('Win64','yes')
    $file = $component.SelectSingleNode('w:File', $ns)
    $file.RemoveAttribute('KeyPath')
    $registry = $harvest.CreateElement('RegistryValue', $ns.LookupNamespace('w'))
    foreach ($pair in @(@('Root','HKCU'), @('Key','Software\Qirato\Installer\Files'), @('Name',$component.Id), @('Type','integer'), @('Value','1'), @('KeyPath','yes'))) { $registry.SetAttribute($pair[0],$pair[1]) }
    [void]$component.AppendChild($registry)
    $remove = $harvest.CreateElement('RemoveFolder', $ns.LookupNamespace('w'))
    $remove.SetAttribute('Id','remove_' + $component.Id); $remove.SetAttribute('On','uninstall')
    [void]$component.AppendChild($remove)
}
foreach ($directory in $harvest.SelectNodes('//w:Directory[not(w:Component)]', $ns)) {
    $remove = $harvest.CreateElement('RemoveFolder', $ns.LookupNamespace('w'))
    $remove.SetAttribute('Id','remove_' + $directory.Id); $remove.SetAttribute('Directory',$directory.Id); $remove.SetAttribute('On','uninstall')
    [void]$harvest.SelectSingleNode('//w:Component', $ns).AppendChild($remove)
}
$harvest.Save($files)
& "$wix\candle.exe" -nologo -arch x64 "-dAppImage=$AppImage" "-dVersion=$Version" "-dIcon=$PSScriptRoot\..\icons\qirato.ico" "-dMarker=$marker" -out "$build\" "$PSScriptRoot\Qirato.wxs" $files
if ($LASTEXITCODE -ne 0) { throw 'Installer compilation failed' }
$output = Join-Path ([IO.Path]::GetFullPath($Destination)) "Qirato-Windows-x64-$Version.msi"
# ICE91 assumes optional per-machine scope; this product explicitly forbids it.
# Keep all other MSI validation (including ICE38 and ICE64) enabled.
& "$wix\light.exe" -nologo -sice:ICE91 -ext WixUIExtension -out $output "$build\Qirato.wixobj" "$build\files.wixobj"
if ($LASTEXITCODE -ne 0) { throw 'Installer linking or validation failed' }
Write-Output $output
