$ErrorActionPreference = "Stop"
Add-Type -AssemblyName UIAutomationClient

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$uninstaller = Join-Path $root "target\installer\Desinstalar-Guías-Física.exe"
$dataDirectory = Join-Path $root "target\uninstaller-choice-test"

if (-not (Test-Path -LiteralPath $uninstaller)) {
    throw "Falta Desinstalar-Guías-Física.exe."
}
if (Test-Path -LiteralPath $dataDirectory) {
    [System.IO.Directory]::Delete($dataDirectory, $true)
}
[System.IO.Directory]::CreateDirectory($dataDirectory) | Out-Null
[System.IO.File]::WriteAllText((Join-Path $dataDirectory "guias.db"), "datos de prueba")
$env:GUIASFISICA_DATA_DIR = $dataDirectory
$env:GUIASFISICA_TEST_NO_INSTALL = "1"

function Invoke-Choice([string]$choice) {
    $rootElement = [System.Windows.Automation.AutomationElement]::RootElement
    $before = @($rootElement.FindAll(
        [System.Windows.Automation.TreeScope]::Children,
        [System.Windows.Automation.Condition]::TrueCondition
    ) | ForEach-Object { $_.Current.NativeWindowHandle })
    $process = Start-Process -FilePath $uninstaller -PassThru
    $deadline = (Get-Date).AddSeconds(10)
    $window = $null
    do {
        Start-Sleep -Milliseconds 200
        $window = $rootElement.FindAll(
            [System.Windows.Automation.TreeScope]::Children,
            [System.Windows.Automation.Condition]::TrueCondition
        ) | Where-Object {
            $before -notcontains $_.Current.NativeWindowHandle -and
            $_.Current.Name -eq "Borrar datos de Guías Física"
        } | Select-Object -First 1
    } until ($window -or (Get-Date) -gt $deadline)
    if (-not $window) {
        if (-not $process.HasExited) { Stop-Process -Id $process.Id -Force }
        throw "No apareció la decisión sobre la base de datos."
    }

    $button = $window.FindAll(
        [System.Windows.Automation.TreeScope]::Descendants,
        [System.Windows.Automation.Condition]::TrueCondition
    ) | Where-Object {
        $_.Current.ControlType -eq [System.Windows.Automation.ControlType]::Button -and
        $_.Current.Name.Replace("&", "") -eq $choice
    } | Select-Object -First 1
    if (-not $button) { throw "No apareció el botón $choice." }
    $button.GetCurrentPattern([System.Windows.Automation.InvokePattern]::Pattern).Invoke()

    if ($choice -eq "Sí") {
        Start-Sleep -Milliseconds 500
        $confirmation = $rootElement.FindAll(
            [System.Windows.Automation.TreeScope]::Children,
            [System.Windows.Automation.Condition]::TrueCondition
        ) | Where-Object { $_.Current.Name -eq "Datos eliminados" } | Select-Object -First 1
        if ($confirmation) {
            $ok = $confirmation.FindAll(
                [System.Windows.Automation.TreeScope]::Descendants,
                [System.Windows.Automation.Condition]::TrueCondition
            ) | Where-Object {
                $_.Current.ControlType -eq [System.Windows.Automation.ControlType]::Button
            } | Select-Object -First 1
            if ($ok) { $ok.GetCurrentPattern([System.Windows.Automation.InvokePattern]::Pattern).Invoke() }
        }
    }

    $process.WaitForExit(5000) | Out-Null
    if (-not $process.HasExited) { Stop-Process -Id $process.Id -Force }
}

Invoke-Choice "No"
if (-not (Test-Path -LiteralPath (Join-Path $dataDirectory "guias.db"))) {
    throw "Elegir No borró la base de datos."
}

Invoke-Choice "Sí"
if (Test-Path -LiteralPath $dataDirectory) {
    throw "Elegir Sí no borró la base de datos."
}

Write-Host "Conservar y borrar datos funcionan correctamente."
