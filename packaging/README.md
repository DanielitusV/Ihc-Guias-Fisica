# Empaquetado para Windows

Requisitos: Windows 10/11, JDK 21 con `jpackage`, Maven 3.9 y WiX 3.14.1. El script puede descargar la distribución portable oficial de WiX dentro de `target/tools`; no la instala globalmente.

```powershell
$env:JAVA_HOME = "C:\ruta\al\jdk-21"
.\packaging\build-installer.ps1 -DownloadWix
```

Si Maven no está en `PATH`:

```powershell
.\packaging\build-installer.ps1 -MavenCommand "C:\ruta\apache-maven\bin\mvn.cmd" -DownloadWix
```

Salidas:

- `target/app-image/Guías Física/Guías Física.exe`: aplicación portable autocontenida.
- `target/installer/Guías Física-1.0.10.exe`: instalador visible para `C:\Program Files\Guías Física`, con accesos directos y desinstalación desde Configuración de Windows.
- `target/installer/Desinstalar-Guías-Física.exe`: desinstalador clickeable con opción para conservar o borrar la base de datos.

La instalación propone `C:\Program Files\Guías Física` y solicita permisos de administrador. La base permanece separada: cada usuario crea y usa `%LOCALAPPDATA%\GuiasFisica\guias.db`.

Para actualizar, ejecuta el instalador de la versión nueva. Windows reemplaza los archivos del programa; la base de datos permanece en `%LOCALAPPDATA%` y se migra automáticamente al abrir la aplicación.

Antes de cambiar el esquema, la aplicación verifica integridad y crea un respaldo completo dentro de `%LOCALAPPDATA%\GuiasFisica\Respaldos`. Si no puede crear o validar el respaldo, la migración no comienza.

El EXE es autocontenido pero no está firmado digitalmente. Puede aparecer SmartScreen por reputación desconocida. Una distribución sin esa advertencia requiere firmar el instalador con un certificado de firma de código emitido por una autoridad confiable.
