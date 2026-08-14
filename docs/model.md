# Modelo del Sistema

## Guide

Representa un tipo de guía de Física que el centro vende.

### Datos

* `id`
* `name`
* `currentPrice`
* `stock`

### Reglas

* El stock no puede ser negativo.
* El precio actual debe ser mayor a 0.
* Al registrar una venta, el stock disminuye en 1.
* Al recibir nuevas guías, el stock aumenta.
* Si el stock es 0, no se puede registrar una venta.
* Cambiar el precio actual no modifica el precio de ventas anteriores.

---

## Sale

Representa la venta de una sola guía.

### Datos

* `id`
* `guideId`
* `price`
* `paymentMethod`
* `status`
* `cancellationReason`
* `createdAt`

### Reglas

* Una venta corresponde a una sola guía.
* El método de pago es `CASH` o `QR`.
* La venta guarda el precio utilizado en el momento en que fue realizada.
* Solo puede registrarse si existe stock disponible.
* Registrar una venta disminuye el stock en 1.
* Registrar una venta genera un ingreso en la cuenta correspondiente.
* Una venta no se elimina del historial.
* Una venta puede ser anulada.
* Al anular una venta, se devuelve 1 unidad al stock.
* Al anular una venta, se revierte el ingreso generado.
* La anulación requiere un motivo obligatorio.
* Una venta anulada permanece visible en el historial.
* `createdAt` guarda la fecha y hora de la venta.

### Estados

* `ACTIVE`
* `CANCELLED`

---

## Order

Representa una llegada de guías desde la fotocopiadora.

### Datos

* `id`
* `paymentCondition`
* `createdAt`

### Reglas

* Un pedido puede contener varios tipos de guía.
* Un pedido debe contener al menos un detalle.
* Al registrar un pedido, las guías recibidas aumentan inmediatamente el stock.
* Las guías pueden recibirse pagadas o fiadas.
* Las guías fiadas generan deuda con el proveedor.
* El pedido no se elimina del historial.
* `createdAt` guarda la fecha y hora del registro de la llegada.

### Condición de pago

* `PAID`: las guías llegaron pagadas.
* `CREDIT`: las guías llegaron fiadas.

La deuda con el proveedor se controla de forma global mediante las llegadas fiadas y los pagos realizados posteriormente.

---

## OrderItem

Representa un tipo de guía incluido dentro de un pedido.

### Datos

* `id`
* `orderId`
* `guideId`
* `quantity`
* `unitCost`

### Reglas

* Cada detalle pertenece a un solo pedido.
* Cada detalle hace referencia a un solo tipo de guía.
* La cantidad debe ser mayor a 0.
* El costo unitario debe ser mayor a 0.
* Un mismo pedido puede contener varios detalles de diferentes tipos de guía.
* El costo del detalle se obtiene mediante:

```text
quantity × unitCost
```

---

## Account

Representa un lugar donde se encuentra el dinero del centro.

### Datos

* `id`
* `name`
* `balance`

### Reglas

* Inicialmente existen dos cuentas:

  * Efectivo
  * QR
* El saldo se actualiza mediante los movimientos de la cuenta.
* Un ingreso aumenta el saldo.
* Una salida disminuye el saldo.
* No se debe modificar el saldo directamente sin registrar el movimiento correspondiente.
* El movimiento y la actualización del saldo deben realizarse dentro de una misma transacción de base de datos.
* Pueden agregarse nuevas cuentas en el futuro.

---

## AccountMovement

Representa una entrada o salida de dinero de una cuenta.

### Datos

* `id`
* `accountId`
* `type`
* `concept`
* `amount`
* `reason`
* `createdAt`

### Reglas

* Todo movimiento pertenece a una cuenta.
* El monto debe ser mayor a 0.
* `INCOME` aumenta el saldo de la cuenta.
* `EXPENSE` disminuye el saldo de la cuenta.
* Una venta genera un movimiento `INCOME`.
* Un gasto genera un movimiento `EXPENSE`.
* Un pago realizado al proveedor genera un movimiento `EXPENSE`.
* Las salidas de dinero deben guardar un motivo.
* El motivo puede ser opcional para los ingresos cuando el concepto ya identifica claramente su origen.
* `createdAt` guarda la fecha y hora del movimiento.
* Un movimiento registrado no debe modificarse sin mantener trazabilidad de la corrección.

### Tipos

* `INCOME`
* `EXPENSE`

### Conceptos iniciales

* `SALE`
* `GENERAL_EXPENSE`
* `SUPPLIER_PAYMENT`
* `SALE_CANCELLATION`
* `CLOSURE_ADJUSTMENT`

---

## SupplierDebt

Representa conceptualmente la deuda pendiente con la fotocopiadora.

La deuda no necesita almacenarse como un saldo independiente si puede calcularse a partir de los registros existentes.

### Cálculo

```text
Deuda pendiente
=
costo de guías recibidas a crédito
-
pagos realizados al proveedor
```

### Reglas

* Una llegada marcada como `CREDIT` aumenta la deuda.
* Una llegada marcada como `PAID` no aumenta la deuda.
* Se permiten pagos parciales al proveedor.
* Un pago disminuye la deuda pendiente.
* La deuda no debe quedar por debajo de 0.
* Los pagos al proveedor quedan registrados como movimientos de dinero.

---

## CashClosure

Representa un cierre de caja en el que se compara lo calculado por el sistema con lo existente realmente.

### Datos

* `id`
* `expectedCash`
* `countedCash`
* `expectedQr`
* `reportedQr`
* `notes`
* `createdAt`

### Reglas

* El cierre compara el dinero esperado con el dinero contado o reportado.
* Efectivo y QR se verifican por separado.
* El efectivo esperado se obtiene de los movimientos registrados en la cuenta de Efectivo.
* El QR esperado se obtiene de los movimientos registrados en la cuenta QR.
* El cierre contiene un conteo físico de cada tipo de guía.
* Una diferencia positiva representa un sobrante.
* Una diferencia negativa representa un faltante.
* El cierre no elimina ventas ni movimientos anteriores.
* El cierre queda guardado permanentemente en el historial.
* `createdAt` guarda la fecha y hora del cierre.
* Las diferencias se calculan a partir de los valores guardados y no necesitan almacenarse como datos independientes.

### Diferencia de efectivo

```text
countedCash - expectedCash
```

### Diferencia de QR

```text
reportedQr - expectedQr
```

---

## CashClosureItem

Representa el conteo físico de un tipo de guía durante un cierre.

### Datos

* `id`
* `cashClosureId`
* `guideId`
* `expectedStock`
* `countedStock`

### Reglas

* Cada registro pertenece a un solo cierre.
* Cada registro corresponde a un solo tipo de guía.
* Cada cierre contiene un registro por cada tipo de guía disponible.
* `expectedStock` representa el stock calculado por el sistema.
* `countedStock` representa las unidades encontradas físicamente.
* La diferencia permite detectar pérdidas, sobrantes o ventas registradas incorrectamente.
* La diferencia se calcula y no necesita almacenarse.

### Diferencia de stock

```text
countedStock - expectedStock
```

---

# Relaciones principales

```text
Guide
 ├── 1:N Sale
 ├── 1:N OrderItem
 └── 1:N CashClosureItem

Order
 └── 1:N OrderItem

Account
 └── 1:N AccountMovement

CashClosure
 └── 1:N CashClosureItem
```

# Flujo principal del sistema

## Venta

```text
Registrar venta
→ comprobar stock
→ guardar venta
→ disminuir stock
→ registrar ingreso
→ actualizar saldo de Efectivo o QR
```

## Llegada de guías

```text
Registrar pedido
→ registrar sus detalles
→ aumentar stock
→ si es CREDIT, aumentar deuda pendiente
```

## Pago al proveedor

```text
Registrar pago
→ registrar salida de dinero
→ actualizar saldo de la cuenta
→ disminuir deuda pendiente
```

## Anulación de venta

```text
Anular venta
→ exigir motivo
→ cambiar estado a CANCELLED
→ devolver guía al stock
→ registrar reversión del dinero
→ actualizar saldo
```

## Cierre de caja

```text
Obtener valores esperados
→ contar guías físicamente
→ contar efectivo
→ reportar QR
→ calcular diferencias
→ registrar cierre
```
