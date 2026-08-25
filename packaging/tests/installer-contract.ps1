$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$script = Get-Content -Raw (Join-Path $root "packaging\build-installer.ps1")

function Require-Text([string]$text, [string]$message) {
    if (-not $script.Contains($text)) { throw $message }
}

Require-Text '$appVersion = "1.0.10"' "El instalador debe usar versión 1.0.10."
Require-Text '$upgradeUuid = "77738e8f-7c6e-3ca4-8d15-ad805df62a0a"' "Debe conservar la identidad de actualización instalada."
Require-Text '--win-upgrade-uuid $upgradeUuid' "El instalador debe actualizar versiones anteriores."
Require-Text '--name "Guías Física"' "El producto debe llamarse Guías Física."
Require-Text '--install-dir "Guías Física"' "La ruta predeterminada debe ser Program Files\Guías Física."
Require-Text '--win-shortcut' "El instalador debe crear acceso directo en el Escritorio."
Require-Text 'Desinstalar-Guías-Física.exe' "Debe generar un desinstalador ejecutable."
Require-Text '--icon $iconPath' "Aplicación e instalador deben usar icono UMSS."
Require-Text '/win32icon:' "El desinstalador debe usar icono UMSS."

if ($script.Contains('--win-per-user-install')) {
    throw "La instalación no debe limitarse al perfil local."
}
if ($script.Contains('--win-shortcut-prompt')) {
    throw "El acceso directo debe crearse siempre, sin volverlo opcional."
}

$source = Join-Path $root "packaging\uninstaller\GuiasFisicaUninstaller.cs"
if (-not (Test-Path -LiteralPath $source)) {
    throw "Falta código fuente del desinstalador."
}

Write-Host "Contrato de empaquetado correcto."
