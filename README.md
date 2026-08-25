# Guías Física

Aplicación JavaFX de escritorio para gestionar ventas, inventario, pedidos, deuda, movimientos de dinero y cierres de caja del Centro de Estudiantes de Física de la UMSS.

## Características

- Interfaz responsive desde 760×560 hasta 1920×1080.
- Tema visual Aero inspirado en Windows 7.
- Dashboard basado en el XLSM con 8 botones de venta rápida: Efectivo y QR para cada guía.
- Ventas por Efectivo o QR/Soto y anulación transaccional.
- Inventario y precios de cuatro guías canónicas.
- Pedidos multiítem pagados o a crédito.
- Gastos, pagos al proveedor y deuda pendiente.
- Cierre diario con conteo de dinero e inventario.
- Historiales filtrables de todas las operaciones.
- SQLite local sin servidor.

## Requisitos de desarrollo

- JDK 21.
- Maven 3.9 o posterior.
- Windows 10/11 para generar el instalador.

Comprueba las herramientas:

```powershell
java -version
mvn -version
```

## Compilar y probar

```powershell
mvn clean verify
```

## Ejecutar desde Maven

```powershell
mvn javafx:run
```

En el primer arranque se solicitarán los precios de Física General, Física I, Física II y Física III.

## Base de datos y respaldo

La base real se guarda en:

```text
%LOCALAPPDATA%\GuiasFisica\guias.db
```

Para respaldarla, cierra la aplicación y copia `guias.db`. No existe ni se utiliza `dev.db` dentro del repositorio.

## Instalador de Windows

```powershell
$env:JAVA_HOME = "C:\ruta\al\jdk-21"
.\packaging\build-installer.ps1 -DownloadWix
```

El resultado queda en `target\installer\Guías Física-1.0.10.exe`. El instalador muestra interfaz, propone `C:\Program Files\Guías Física`, solicita permisos de administrador y crea accesos directos en Escritorio y menú Inicio. Incluye un runtime Java privado; el equipo destino no necesita instalar Java. Consulta [packaging/README.md](packaging/README.md) para rutas y opciones.

Para entregar la aplicación, comparte solamente ese `.exe` (por USB o un enlace de Drive). La otra persona hace doble clic, sigue el asistente y abre el acceso directo; no necesita Java, Maven, Excel ni configurar una base de datos.

El instalador generado localmente no tiene firma digital comercial. Windows SmartScreen o un antivirus pueden advertir sobre un ejecutable nuevo y poco descargado; no se puede garantizar que nunca aparezca ese aviso. Para eliminarlo de forma profesional hay que firmar el `.exe` con un certificado de firma de código confiable.

## Desinstalar

Ejecuta `target\installer\Desinstalar-Guías-Física.exe`. El programa permite conservar la base para una futura actualización o borrarla definitivamente. También permanece disponible la desinstalación estándar desde **Configuración de Windows → Aplicaciones → Aplicaciones instaladas**.

## Datos de la hoja anterior

`Guias-2026-1.xlsm` se usó como referencia del flujo operativo. La aplicación no importa sus registros ni sus fórmulas: empieza con una base limpia para evitar arrastrar inconsistencias históricas.

La versión web anterior se conserva en la rama `legacy-web`.
