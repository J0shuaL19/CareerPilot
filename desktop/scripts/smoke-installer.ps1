#requires -Version 7.4

[CmdletBinding()]
param(
    [Parameter()]
    [string]$InstallerPath = (Join-Path $PSScriptRoot '..\src-tauri\target\release\bundle\nsis\CareerPilot_0.1.0_x64-setup.exe'),

    [Parameter()]
    [ValidateRange(1, 300)]
    [int]$InstallerTimeoutSeconds = 120
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true

function Assert-Equal {
    param(
        [Parameter(Mandatory)]$Actual,
        [Parameter(Mandatory)]$Expected,
        [Parameter(Mandatory)][string]$Description
    )

    if ($Actual -ne $Expected) {
        throw "$Description. Expected '$Expected', received '$Actual'."
    }
}

function Assert-PathExists {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$Description
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "$Description was not created: $Path"
    }
}

function Invoke-CheckedProcess {
    param(
        [Parameter(Mandatory)][string]$FilePath,
        [Parameter()][string[]]$ArgumentList = @(),
        [Parameter(Mandatory)][int]$TimeoutSeconds,
        [Parameter(Mandatory)][string]$Description
    )

    $startInfo = [Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $FilePath
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    foreach ($argument in $ArgumentList) {
        $startInfo.ArgumentList.Add($argument)
    }

    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    if (-not $process.Start()) {
        throw "$Description did not start."
    }
    if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
        $process.Kill($true)
        throw "$Description did not finish within $TimeoutSeconds seconds."
    }
    if ($process.ExitCode -ne 0) {
        throw "$Description failed with exit code $($process.ExitCode)."
    }
}

function Remove-TestDirectory {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$AllowedRoot
    )

    $resolvedPath = [IO.Path]::GetFullPath($Path)
    $resolvedRoot = [IO.Path]::GetFullPath($AllowedRoot)
    $rootPrefix = $resolvedRoot.TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    if (-not $resolvedPath.StartsWith($rootPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove a directory outside the installer-smoke root: $resolvedPath"
    }
    if ([IO.Directory]::Exists($resolvedPath)) {
        [IO.Directory]::Delete($resolvedPath, $true)
    }
}

$resolvedInstaller = [IO.Path]::GetFullPath($InstallerPath)
if (-not [IO.File]::Exists($resolvedInstaller)) {
    throw "CareerPilot installer was not found: $resolvedInstaller"
}

$uninstallKey = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\CareerPilot'
$productKey = 'HKCU:\Software\careerpilot\CareerPilot'
$desktopShortcut = Join-Path ([Environment]::GetFolderPath('Desktop')) 'CareerPilot.lnk'
$startMenuShortcut = Join-Path ([Environment]::GetFolderPath('Programs')) 'CareerPilot.lnk'
$existingInstances = @(Get-Process -Name 'CareerPilot' -ErrorAction SilentlyContinue)

if ($existingInstances.Count -gt 0) {
    throw 'Close the running CareerPilot app before starting the installer smoke test.'
}
foreach ($reservedPath in @($uninstallKey, $productKey, $desktopShortcut, $startMenuShortcut)) {
    if (Test-Path -LiteralPath $reservedPath) {
        throw "The installer smoke test will not overwrite an existing CareerPilot installation artifact: $reservedPath"
    }
}

$smokeBase = Join-Path ([IO.Path]::GetTempPath()) 'CareerPilotInstallerSmoke'
$smokeRoot = Join-Path $smokeBase ([guid]::NewGuid().ToString('N'))
$installRoot = Join-Path $smokeRoot 'install'
$installedExecutable = Join-Path $installRoot 'CareerPilot.exe'
$installedBackend = Join-Path $installRoot 'backend\careerpilot-backend.jar'
$installedJava = Join-Path $installRoot 'runtime\bin\java.exe'
$uninstaller = Join-Path $installRoot 'uninstall.exe'
$installationCreated = $false

try {
    [IO.Directory]::CreateDirectory($smokeRoot) | Out-Null

    Write-Host "Silently installing CareerPilot to $installRoot"
    Invoke-CheckedProcess -FilePath $resolvedInstaller -ArgumentList @('/S', "/D=$installRoot") -TimeoutSeconds $InstallerTimeoutSeconds -Description 'CareerPilot installer'
    $installationCreated = $true

    Assert-PathExists -Path $installedExecutable -Description 'Installed CareerPilot executable'
    Assert-PathExists -Path $installedBackend -Description 'Installed backend JAR'
    Assert-PathExists -Path $installedJava -Description 'Installed Java runtime'
    Assert-PathExists -Path $uninstaller -Description 'CareerPilot uninstaller'
    Assert-PathExists -Path $desktopShortcut -Description 'Desktop shortcut'
    Assert-PathExists -Path $startMenuShortcut -Description 'Start menu shortcut'
    Assert-PathExists -Path $uninstallKey -Description 'Uninstall registry entry'

    $uninstallEntry = Get-ItemProperty -LiteralPath $uninstallKey
    $registeredInstallLocation = ([string]$uninstallEntry.InstallLocation).Trim('"')
    Assert-Equal -Actual ([IO.Path]::GetFullPath($registeredInstallLocation)) -Expected ([IO.Path]::GetFullPath($installRoot)) -Description 'Registered install location is incorrect'
    Assert-Equal -Actual $uninstallEntry.DisplayName -Expected 'CareerPilot' -Description 'Registered display name is incorrect'
    Assert-Equal -Actual $uninstallEntry.DisplayVersion -Expected '0.1.0' -Description 'Registered display version is incorrect'

    & (Join-Path $PSScriptRoot 'smoke-packaged-app.ps1') -ExecutablePath $installedExecutable

    Write-Host 'Silently uninstalling CareerPilot...'
    Invoke-CheckedProcess -FilePath $uninstaller -ArgumentList @('/S') -TimeoutSeconds $InstallerTimeoutSeconds -Description 'CareerPilot uninstaller'
    $installationCreated = $false

    if (Test-Path -LiteralPath $installRoot) {
        throw "The uninstall left the installation directory behind: $installRoot"
    }
    foreach ($removedPath in @($uninstallKey, $desktopShortcut, $startMenuShortcut)) {
        if (Test-Path -LiteralPath $removedPath) {
            throw "The uninstall left a registered installation artifact behind: $removedPath"
        }
    }

    Write-Host 'PASS: installer installed, exercised, and uninstalled CareerPilot cleanly.'
}
finally {
    $testProcesses = @(Get-CimInstance Win32_Process | Where-Object {
        ($_.ExecutablePath -and $_.ExecutablePath.StartsWith($installRoot, [StringComparison]::OrdinalIgnoreCase)) -or
        ($_.CommandLine -and $_.CommandLine.Contains($installRoot, [StringComparison]::OrdinalIgnoreCase))
    })
    foreach ($testProcess in $testProcesses) {
        Stop-Process -Id $testProcess.ProcessId -Force -ErrorAction SilentlyContinue
    }

    if ($installationCreated -and [IO.File]::Exists($uninstaller)) {
        try {
            Invoke-CheckedProcess -FilePath $uninstaller -ArgumentList @('/S') -TimeoutSeconds $InstallerTimeoutSeconds -Description 'CareerPilot cleanup uninstaller'
        }
        catch {
            Write-Warning $_
        }
    }

    foreach ($testShortcut in @($desktopShortcut, $startMenuShortcut)) {
        if (Test-Path -LiteralPath $testShortcut) {
            Remove-Item -LiteralPath $testShortcut -Force
        }
    }
    foreach ($testRegistryKey in @($uninstallKey, $productKey)) {
        if (Test-Path -LiteralPath $testRegistryKey) {
            Remove-Item -LiteralPath $testRegistryKey -Recurse -Force
        }
    }

    if ([IO.Directory]::Exists($installRoot)) {
        Remove-TestDirectory -Path $installRoot -AllowedRoot $smokeBase
    }
    if ([IO.Directory]::Exists($smokeRoot)) {
        Remove-TestDirectory -Path $smokeRoot -AllowedRoot $smokeBase
    }
}
