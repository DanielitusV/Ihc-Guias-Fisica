# Backend Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Completar backend no-UI con persistencia SQLite, transacciones, cierres, estado diario, consultas y bootstrap idempotente.

**Architecture:** Extender diseño feature-first existente. Reglas quedan en dominio/services; repositories JDBC hacen persistencia y consultas; transaction services coordinan operaciones atómicas. Bootstrap resuelve DB persistente y siembra solo cuentas canónicas, sin inventar precios.

**Tech Stack:** Java 21, Maven, SQLite JDBC, JUnit 5.

## Global Constraints

- No JavaFX, FXML, CSS, controllers, vistas, instalador, web, Supabase ni importación Excel.
- Dinero usa `BigDecimal`; SQLite usa `NUMERIC`.
- `PRAGMA foreign_keys = ON` en cada conexión.
- No tablas `supplier_debt` ni `day_status`.
- Preservar cambios locales existentes y comportamiento cubierto por 76 tests.

---

### Task 1: Venta segura y consultas

**Files:**
- Modify: `src/main/resources/schema.sql`
- Modify: `src/main/java/com/litus/guias/sale/Sale.java`
- Modify: `src/main/java/com/litus/guias/persistence/SaleRepository.java`
- Modify: `src/main/java/com/litus/guias/persistence/SaleTransactionService.java`
- Test: `src/test/java/com/litus/guias/persistence/SaleTransactionServiceTest.java`
- Test: `src/test/java/com/litus/guias/persistence/SaleRepositoryTest.java`

**Interfaces:**
- Produce: venta guarda `accountId`; `cancelSale(long saleId, String reason, LocalDateTime createdAt)` deriva cuenta desde venta; listados por fecha/rango.

- [ ] Escribir test donde cancelación revierte cuenta original aunque exista otra cuenta.
- [ ] Ejecutar test y verificar fallo por API/modelo inseguro.
- [ ] Añadir `account_id` a venta, mapping y consultas; eliminar dependencia externa en cancelación.
- [ ] Ejecutar tests focales y suite completa.

### Task 2: Repositories de lectura para futura UI

**Files:**
- Modify: `GuideRepository.java`, `AccountRepository.java`, `AccountMovementRepository.java`, `OrderRepository.java`, `SaleRepository.java`, `CashClosureRepository.java`
- Test: tests de repository correspondientes.

**Interfaces:**
- Produce: `findAll`, filtros por fecha/rango/concepto y carga completa de items.

- [ ] Escribir tests de resultados, orden estable y límites de fecha.
- [ ] Ejecutar tests y verificar fallos por métodos ausentes.
- [ ] Implementar consultas JDBC simples y mappings compartidos.
- [ ] Ejecutar tests focales y suite completa.

### Task 3: Flujo persistente de cierre

**Files:**
- Create: `src/main/java/com/litus/guias/persistence/CashClosureTransactionService.java`
- Modify: `CashClosureRepository.java`
- Test: `CashClosureTransactionServiceTest.java`, `CashClosureRepositoryTest.java`

**Interfaces:**
- Produce: crear cierre atómico, cancelar con motivo, buscar válido por fecha, historial con items.

- [ ] Escribir tests de cierre único, nuevo cierre tras cancelación, rollback de items y doble cancelación.
- [ ] Ejecutar tests y verificar fallos esperados.
- [ ] Coordinar `CashClosureService` y repository dentro de `Database.inTransaction`.
- [ ] Ejecutar tests focales y suite completa.

### Task 4: Estado diario derivado desde SQLite

**Files:**
- Create: `src/main/java/com/litus/guias/persistence/DayStatusQueryService.java`
- Test: `src/test/java/com/litus/guias/persistence/DayStatusQueryServiceTest.java`

**Interfaces:**
- Produce: `DayStatus getStatus(LocalDate day, LocalDate today)` usando cierre válido y actividad real de ventas, pedidos y movimientos.

- [ ] Escribir tests de `CLOSED`, `MISSED`, `NO_ACTIVITY`, `OPEN` y cierre cancelado.
- [ ] Ejecutar tests y verificar fallo por servicio ausente.
- [ ] Consultar actividad mediante `EXISTS` y delegar decisión a `DayStatusCalculator`.
- [ ] Ejecutar tests focales y suite completa.

### Task 5: Deuda y pagos robustos

**Files:**
- Modify: `SupplierDebtQueryService.java`, `SupplierPaymentTransactionService.java`, `AccountMovementConcept.java`
- Test: tests de deuda/pago.

**Interfaces:**
- Produce: deuda derivada no negativa; pago no puede exceder deuda; enum Java incluye `OTHER`.

- [ ] Escribir tests de pedido `PAID`, pagos parciales y sobrepago con rollback.
- [ ] Ejecutar tests y verificar fallos correctos.
- [ ] Validar deuda dentro de misma transacción antes de persistir pago.
- [ ] Ejecutar tests focales y suite completa.

### Task 6: Bootstrap de primera ejecución

**Files:**
- Create: `src/main/java/com/litus/guias/persistence/ApplicationDataPath.java`
- Create: `src/main/java/com/litus/guias/persistence/ApplicationBootstrap.java`
- Test: `src/test/java/com/litus/guias/persistence/ApplicationBootstrapTest.java`

**Interfaces:**
- Produce: ruta `%LOCALAPPDATA%/GuiasFisica/guias.db`; inicialización idempotente; cuentas `Efectivo` y `QR / Soto`; API backend para guías con precios provistos.

- [ ] Escribir tests con directorio temporal y proveedor de entorno inyectado.
- [ ] Ejecutar tests y verificar fallo por clases ausentes.
- [ ] Crear directorio, schema y seed con `INSERT ... ON CONFLICT DO NOTHING`; no sembrar precios.
- [ ] Reabrir DB y verificar que datos persisten sin duplicación.

### Task 7: Verificación final

- [ ] Buscar `@Disabled`, `double`, `float`, UI nueva y tablas prohibidas.
- [ ] Ejecutar `mvn clean test` con Java 21.
- [ ] Ejecutar `mvn package`.
- [ ] Revisar `git diff` para preservar cambios previos y evitar archivos generados.

