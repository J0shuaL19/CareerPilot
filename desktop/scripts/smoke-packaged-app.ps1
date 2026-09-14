#requires -Version 7.4

[CmdletBinding()]
param(
    [Parameter()]
    [string]$ExecutablePath = (Join-Path $PSScriptRoot '..\src-tauri\target\release\CareerPilot.exe'),

    [Parameter()]
    [ValidateRange(1, 300)]
    [int]$StartupTimeoutSeconds = 60
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

function Wait-ForBackendPort {
    param(
        [Parameter(Mandatory)][System.Diagnostics.Process]$Process,
        [Parameter(Mandatory)][string]$LauncherLog,
        [Parameter(Mandatory)][datetime]$Deadline
    )

    while ([datetime]::UtcNow -lt $Deadline) {
        $Process.Refresh()
        if ($Process.HasExited) {
            throw "CareerPilot exited before the bundled backend became ready (exit code $($Process.ExitCode))."
        }

        if (Test-Path -LiteralPath $LauncherLog) {
            $logContent = Get-Content -Raw -LiteralPath $LauncherLog
            if ($logContent -match '(?m)^Port:\s*(\d+)\s*$') {
                return [int]$Matches[1]
            }
        }

        Start-Sleep -Milliseconds 250
    }

    throw "CareerPilot did not record its backend port within $StartupTimeoutSeconds seconds."
}

function Wait-ForHealth {
    param(
        [Parameter(Mandatory)][uri]$BaseUri,
        [Parameter(Mandatory)][System.Diagnostics.Process]$Process,
        [Parameter(Mandatory)][datetime]$Deadline
    )

    while ([datetime]::UtcNow -lt $Deadline) {
        $Process.Refresh()
        if ($Process.HasExited) {
            throw "CareerPilot exited while waiting for the health endpoint (exit code $($Process.ExitCode))."
        }

        try {
            $healthUri = [uri]::new($BaseUri, 'api/health')
            return Invoke-RestMethod -Uri $healthUri -TimeoutSec 2
        }
        catch {
            Start-Sleep -Milliseconds 250
        }
    }

    throw "The bundled backend did not become healthy within $StartupTimeoutSeconds seconds."
}

function Wait-ForProcessExit {
    param(
        [Parameter(Mandatory)][int]$ProcessId,
        [Parameter(Mandatory)][datetime]$Deadline
    )

    while ([datetime]::UtcNow -lt $Deadline) {
        if (-not (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) {
            return
        }
        Start-Sleep -Milliseconds 100
    }

    throw "Process $ProcessId did not exit before the timeout."
}

$resolvedExecutable = [IO.Path]::GetFullPath($ExecutablePath)
if (-not [IO.File]::Exists($resolvedExecutable)) {
    throw "Packaged CareerPilot executable was not found: $resolvedExecutable"
}

$existingInstances = @(Get-Process -Name 'CareerPilot' -ErrorAction SilentlyContinue)
if ($existingInstances.Count -gt 0) {
    throw 'Close the running CareerPilot app before starting the packaged-app smoke test.'
}

$smokeRoot = Join-Path ([IO.Path]::GetTempPath()) ("CareerPilotSmoke\{0}" -f [guid]::NewGuid().ToString('N'))
$launcherLog = Join-Path $smokeRoot 'logs\launcher.log'
$appProcess = $null
$backendProcesses = @()

try {
    [IO.Directory]::CreateDirectory($smokeRoot) | Out-Null
    $environment = @{
        CAREERPILOT_DESKTOP_HOME = $smokeRoot
        OPENAI_API_KEY = 'careerpilot-smoke-secret-must-not-be-exported'
    }

    Write-Host "Starting packaged CareerPilot from $resolvedExecutable"
    $appProcess = Start-Process -FilePath $resolvedExecutable -PassThru -Environment $environment
    $deadline = [datetime]::UtcNow.AddSeconds($StartupTimeoutSeconds)

    $port = Wait-ForBackendPort -Process $appProcess -LauncherLog $launcherLog -Deadline $deadline
    $baseUri = [uri]"http://127.0.0.1:$port"
    $backendProcesses = @(Get-CimInstance Win32_Process -Filter "ParentProcessId = $($appProcess.Id)" |
        Where-Object { $_.Name -in @('java.exe', 'javaw.exe') })
    Assert-Equal -Actual $backendProcesses.Count -Expected 1 -Description 'Bundled Java backend process count is incorrect'

    $health = Wait-ForHealth -BaseUri $baseUri -Process $appProcess -Deadline $deadline

    Assert-Equal -Actual $health.status -Expected 'ok' -Description 'Health status is incorrect'
    Assert-Equal -Actual $health.service -Expected 'careerpilot-backend' -Description 'Health service is incorrect'

    Write-Host 'Verifying the desktop data-transfer round trip...'
    $capabilitiesUri = [uri]::new($baseUri, 'api/data-transfer/capabilities')
    $jobsUri = [uri]::new($baseUri, 'api/jobs')
    $exportUri = [uri]::new($baseUri, 'api/data-transfer/export')
    $previewUri = [uri]::new($baseUri, 'api/data-transfer/import/preview')
    $importUri = [uri]::new($baseUri, 'api/data-transfer/import')
    $archivePath = Join-Path $smokeRoot 'careerpilot-export.json'
    $secretMarker = $environment.OPENAI_API_KEY
    $recordMarker = [guid]::NewGuid().ToString('N')

    $capabilities = Invoke-RestMethod -Uri $capabilitiesUri -TimeoutSec 5
    Assert-Equal -Actual $capabilities.importEnabled -Expected $true -Description 'Desktop import capability is incorrect'
    Assert-Equal -Actual $capabilities.formatVersion -Expected 1 -Description 'Archive format version is incorrect'

    $sourceJobRequest = @{
        company = "Smoke Source $recordMarker"
        title = 'Software Engineer'
        description = 'Packaged-app data-transfer smoke test source record.'
        jobUrl = 'https://example.com/careerpilot-smoke-source'
    } | ConvertTo-Json
    $sourceJob = Invoke-RestMethod -Method Post -Uri $jobsUri -ContentType 'application/json' -Body $sourceJobRequest -TimeoutSec 5
    Assert-Equal -Actual $sourceJob.company -Expected "Smoke Source $recordMarker" -Description 'Source job was not created'

    Invoke-WebRequest -Uri $exportUri -OutFile $archivePath -TimeoutSec 10 | Out-Null
    $archiveText = Get-Content -Raw -LiteralPath $archivePath
    if ($archiveText.Contains($secretMarker, [StringComparison]::Ordinal)) {
        throw 'The exported archive contains the API-key marker.'
    }
    $archive = $archiveText | ConvertFrom-Json
    Assert-Equal -Actual $archive.format -Expected 'careerpilot-data' -Description 'Archive format is incorrect'
    Assert-Equal -Actual $archive.version -Expected 1 -Description 'Archive version is incorrect'
    Assert-Equal -Actual @($archive.data.jobs).Count -Expected 1 -Description 'Exported job count is incorrect'

    $temporaryJobRequest = @{
        company = "Smoke Temporary $recordMarker"
        title = 'Delete During Import'
        description = 'This record must be replaced by the imported archive.'
        jobUrl = 'https://example.com/careerpilot-smoke-temporary'
    } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri $jobsUri -ContentType 'application/json' -Body $temporaryJobRequest -TimeoutSec 5 | Out-Null

    $archiveFile = Get-Item -LiteralPath $archivePath
    $preview = Invoke-RestMethod -Method Post -Uri $previewUri -Form @{ file = $archiveFile } -TimeoutSec 10
    Assert-Equal -Actual $preview.incoming.jobs -Expected 1 -Description 'Preview incoming job count is incorrect'
    Assert-Equal -Actual $preview.existing.jobs -Expected 2 -Description 'Preview existing job count is incorrect'
    Assert-Equal -Actual $preview.willReplaceExistingData -Expected $true -Description 'Preview replacement flag is incorrect'

    $importResult = Invoke-RestMethod -Method Post -Uri $importUri -Form @{ file = $archiveFile } -TimeoutSec 15
    Assert-Equal -Actual $importResult.imported.jobs -Expected 1 -Description 'Imported job count is incorrect'

    $restoredJobs = @(Invoke-RestMethod -Uri $jobsUri -TimeoutSec 5)
    Assert-Equal -Actual $restoredJobs.Count -Expected 1 -Description 'Restored job count is incorrect'
    Assert-Equal -Actual $restoredJobs[0].company -Expected "Smoke Source $recordMarker" -Description 'Restored source job is incorrect'
    Assert-Equal -Actual $restoredJobs[0].title -Expected 'Software Engineer' -Description 'Restored source job title is incorrect'

    Write-Host 'Closing the CareerPilot window...'
    if (-not $appProcess.CloseMainWindow()) {
        throw 'CareerPilot did not accept the main-window close request.'
    }

    if (-not $appProcess.WaitForExit(15000)) {
        throw 'CareerPilot did not exit within 15 seconds after its window was closed.'
    }

    foreach ($backendProcess in $backendProcesses) {
        Wait-ForProcessExit -ProcessId $backendProcess.ProcessId -Deadline ([datetime]::UtcNow.AddSeconds(15))
    }

    Write-Host 'PASS: packaged app lifecycle and desktop data-transfer round trip succeeded.'
}
catch {
    if (Test-Path -LiteralPath $launcherLog) {
        Write-Warning "Launcher log:`n$(Get-Content -Raw -LiteralPath $launcherLog)"
    }
    throw
}
finally {
    if ($backendProcesses.Count -eq 0 -and $null -ne $appProcess) {
        $backendProcesses = @(Get-CimInstance Win32_Process -Filter "ParentProcessId = $($appProcess.Id)" |
            Where-Object { $_.Name -in @('java.exe', 'javaw.exe') })
    }

    if ($null -ne $appProcess) {
        $appProcess.Refresh()
        if (-not $appProcess.HasExited) {
            Stop-Process -Id $appProcess.Id -Force -ErrorAction SilentlyContinue
        }
    }

    foreach ($backendProcess in $backendProcesses) {
        Stop-Process -Id $backendProcess.ProcessId -Force -ErrorAction SilentlyContinue
    }

    for ($attempt = 1; $attempt -le 20 -and [IO.Directory]::Exists($smokeRoot); $attempt++) {
        try {
            [IO.Directory]::Delete($smokeRoot, $true)
        }
        catch {
            if ($attempt -eq 20) {
                Write-Warning "Could not remove the isolated smoke-test directory: $smokeRoot"
            }
            else {
                Start-Sleep -Milliseconds 100
            }
        }
    }
}
