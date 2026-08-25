[CmdletBinding()]
param(
    [string]$MavenCommand = "mvn",
    [switch]$DownloadWix
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$targetRoot = Join-Path $projectRoot "target"
$installerDir = Join-Path $targetRoot "installer"
$appImageDir = Join-Path $targetRoot "app-image"
$wixDir = Join-Path $targetRoot "tools\wix314"
$wixZip = Join-Path $targetRoot "tools\wix314-binaries.zip"
$appVersion = "1.0.10"
$upgradeUuid = "77738e8f-7c6e-3ca4-8d15-ad805df62a0a"
$iconPath = Join-Path $projectRoot "packaging\assets\GuiasFisica.ico"
$uninstallerSource = Join-Path $projectRoot "packaging\uninstaller\GuiasFisicaUninstaller.cs"
$frameworkCompiler = Join-Path $env:WINDIR "Microsoft.NET\Framework64\v4.0.30319\csc.exe"

foreach ($path in @($installerDir, $appImageDir, $wixDir)) {
    $absolute = [System.IO.Path]::GetFullPath($path)
    if (-not $absolute.StartsWith($projectRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Ruta de salida fuera del proyecto: $absolute"
    }
}

if (-not $env:JAVA_HOME) { throw "JAVA_HOME debe apuntar a un JDK 21 con jpackage." }
if (-not (Test-Path -LiteralPath $iconPath)) { throw "No se encontró el icono en $iconPath" }
$jpackage = Join-Path $env:JAVA_HOME "bin\jpackage.exe"
if (-not (Test-Path -LiteralPath $jpackage)) { throw "No se encontró jpackage en $jpackage" }

& $MavenCommand clean package
if ($LASTEXITCODE -ne 0) { throw "Maven falló." }

$mainJar = "guias-fisica-1.0-SNAPSHOT.jar"
$inputDir = Join-Path $targetRoot "package-input"
if (-not (Test-Path -LiteralPath (Join-Path $inputDir $mainJar))) { throw "No se preparó el JAR principal para jpackage." }

if (Test-Path -LiteralPath $appImageDir) { Remove-Item -LiteralPath $appImageDir -Recurse -Force }
New-Item -ItemType Directory -Path $appImageDir | Out-Null

& $jpackage --type app-image --name "Guías Física" --app-version $appVersion `
    --vendor "Centro de Estudiantes de Física - UMSS" `
    --description "Gestión de ventas, inventario, pedidos y cierres de guías" `
    --input $inputDir --main-jar $mainJar --main-class "com.litus.guias.Launcher" `
    --java-options "-Dfile.encoding=UTF-8" --icon $iconPath --dest $appImageDir
if ($LASTEXITCODE -ne 0) { throw "No se pudo crear la aplicación autocontenida." }

$candle = Get-Command candle.exe -ErrorAction SilentlyContinue
$light = Get-Command light.exe -ErrorAction SilentlyContinue
if ((-not $candle -or -not $light) -and (Test-Path -LiteralPath (Join-Path $wixDir "candle.exe"))) {
    $env:Path = "$wixDir;$env:Path"
    $candle = Get-Command candle.exe -ErrorAction SilentlyContinue
    $light = Get-Command light.exe -ErrorAction SilentlyContinue
}
if ((-not $candle -or -not $light) -and $DownloadWix) {
    New-Item -ItemType Directory -Force -Path (Split-Path $wixZip) | Out-Null
    $uri = "https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip"
    Write-Host "Descargando WiX 3.14.1 portable desde su release oficial..."
    Invoke-WebRequest -Uri $uri -OutFile $wixZip
    if (Test-Path -LiteralPath $wixDir) { Remove-Item -LiteralPath $wixDir -Recurse -Force }
    Expand-Archive -LiteralPath $wixZip -DestinationPath $wixDir
    $env:Path = "$wixDir;$env:Path"
    $candle = Get-Command candle.exe -ErrorAction SilentlyContinue
    $light = Get-Command light.exe -ErrorAction SilentlyContinue
}
if (-not $candle -or -not $light) {
    throw "Falta WiX 3 (candle.exe/light.exe). Ejecuta con -DownloadWix o instala WiX 3.14.1. La app-image sí quedó en $appImageDir"
}

if (Test-Path -LiteralPath $installerDir) { Remove-Item -LiteralPath $installerDir -Recurse -Force }
New-Item -ItemType Directory -Path $installerDir | Out-Null

& $jpackage --type exe --name "Guías Física" --app-version $appVersion `
    --vendor "Centro de Estudiantes de Física - UMSS" `
    --description "Gestión de ventas, inventario, pedidos y cierres de guías" `
    --input $inputDir --main-jar $mainJar --main-class "com.litus.guias.Launcher" `
    --java-options "-Dfile.encoding=UTF-8" --icon $iconPath --dest $installerDir `
    --win-upgrade-uuid $upgradeUuid `
    --install-dir "Guías Física" `
    --win-menu --win-menu-group "Guías Física" --win-shortcut
if ($LASTEXITCODE -ne 0) { throw "jpackage no pudo crear el instalador EXE." }

$installer = Get-ChildItem -LiteralPath $installerDir -Filter *.exe | Select-Object -First 1
if (-not $installer) { throw "jpackage terminó sin producir un EXE." }

if (-not (Test-Path -LiteralPath $frameworkCompiler)) {
    throw "No se encontró el compilador de .NET Framework en $frameworkCompiler"
}
$uninstaller = Join-Path $installerDir "Desinstalar-Guías-Física.exe"
& $frameworkCompiler /nologo /target:winexe /optimize+ "/win32icon:$iconPath" `
    "/out:$uninstaller" /reference:System.dll /reference:System.Windows.Forms.dll `
    $uninstallerSource
if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $uninstaller)) {
    throw "No se pudo crear el desinstalador EXE."
}

Write-Host "Instalador creado: $($installer.FullName)"
Write-Host "Desinstalador creado: $uninstaller"
Write-Host "Aplicación autocontenida: $(Join-Path $appImageDir 'Guías Física\Guías Física.exe')"
