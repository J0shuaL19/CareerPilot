[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true

$desktopRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$repositoryRoot = [IO.Path]::GetFullPath((Join-Path $desktopRoot '..'))
$frontendRoot = Join-Path $repositoryRoot 'frontend'
$backendRoot = Join-Path $repositoryRoot 'backend'
$resourcesRoot = [IO.Path]::GetFullPath((Join-Path $desktopRoot 'src-tauri\resources'))
$backendResources = Join-Path $resourcesRoot 'backend'
$runtimeResources = Join-Path $resourcesRoot 'runtime'
$moduleListPath = Join-Path $desktopRoot 'runtime-modules.txt'

function Reset-GeneratedDirectory {
    param([Parameter(Mandatory)][string]$Path)

    $resolvedPath = [IO.Path]::GetFullPath($Path)
    $resourcesPrefix = $resourcesRoot + [IO.Path]::DirectorySeparatorChar
    if (-not $resolvedPath.StartsWith($resourcesPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to reset a directory outside the desktop resources root: $resolvedPath"
    }

    if ([IO.Directory]::Exists($resolvedPath)) {
        [IO.Directory]::Delete($resolvedPath, $true)
    }
    [IO.Directory]::CreateDirectory($resolvedPath) | Out-Null
}

Write-Host 'Building the CareerPilot frontend...'
& pnpm --dir $frontendRoot build

Write-Host 'Packaging the CareerPilot desktop backend...'
& mvn --file (Join-Path $backendRoot 'pom.xml') clean --activate-profiles desktop package -DskipTests

$backendJars = @(Get-ChildItem -LiteralPath (Join-Path $backendRoot 'target') -Filter 'backend-*.jar' |
    Where-Object { $_.Name -notlike '*.original' })
if ($backendJars.Count -ne 1) {
    throw "Expected exactly one packaged backend JAR, found $($backendJars.Count)."
}

$moduleLines = @(Get-Content -LiteralPath $moduleListPath |
    Where-Object { $_ -notmatch '^\s*(#|$)' })
$modules = ($moduleLines -join '').Trim()
if ([string]::IsNullOrWhiteSpace($modules)) {
    throw 'The Java runtime module list is empty.'
}

Reset-GeneratedDirectory -Path $backendResources
Reset-GeneratedDirectory -Path $runtimeResources
[IO.Directory]::Delete($runtimeResources, $false)

$packagedBackend = Join-Path $backendResources 'careerpilot-backend.jar'
[IO.File]::Copy($backendJars[0].FullName, $packagedBackend, $true)

$jlink = (Get-Command 'jlink.exe' -ErrorAction Stop).Source
$jlinkArguments = @(
    '--add-modules', $modules,
    '--output', $runtimeResources,
    '--strip-debug',
    '--no-header-files',
    '--no-man-pages',
    '--compress=zip-6'
)
Write-Host 'Creating the bundled Java runtime...'
& $jlink @jlinkArguments

$requiredFiles = @(
    $packagedBackend,
    (Join-Path $runtimeResources 'bin\java.exe'),
    (Join-Path $runtimeResources 'bin\javaw.exe')
)
foreach ($requiredFile in $requiredFiles) {
    if (-not [IO.File]::Exists($requiredFile)) {
        throw "Required desktop resource was not created: $requiredFile"
    }
}

$runtimeBytes = (Get-ChildItem -LiteralPath $runtimeResources -Recurse -File |
    Measure-Object -Property Length -Sum).Sum
Write-Host ('Desktop resources are ready: backend {0:N1} MB, runtime {1:N1} MB.' -f
    ($backendJars[0].Length / 1MB), ($runtimeBytes / 1MB))