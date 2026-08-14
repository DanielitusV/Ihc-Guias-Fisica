# Modelo del Sistema

## Guide

Representa un tipo de guía de Física que el centro vende.

### Datos

- `id`
- `name`
- `currentPrice`
- `stock`

### Reglas

- El stock no puede ser negativo.
- El precio actual debe ser mayor a 0.
- Al registrar una venta, el stock disminuye en 1.
- Al recibir nuevas guías, el stock aumenta.
- Si el stock es 0, no se puede registrar una venta.

***

## Sale

Representa la venta de una sola guía

### Datos

- `id`
- `guideId`
- `price`
- `paymentMethod`
- `date`
- `status`
- `cancellationReason`

### Reglas
- Una venta corresponde a una sola guía.
- El método de pago es `Efectivo` o `QR`.
- Guarda el precio usado al momento de vender.
- Solo puede registrarse si existe stock.
- Registrar una venta disminuye el stock en 1.
- Una venta no se elimina: puede anularse.
- Al anularla, se devuelve 1 unidad al stock.
- La anulación requiere un motivo obligatorio.

***

## Order

Representa una llegada de guías desde la fotocopiadora.

### Datos

- `id`
- `date`
- `status`

### Reglas

- Un pedido puede contener varios tipos de guía.
- Al registrarlo, las guías recibidas aumentan el stock.
- Un pedido puede quedar pendiente de pago.
- No se elimina del historial.

***

## OrderItem

Representa un tipo de guía incluido dentro de un pedido

### Datos

- `id`
- `orderId`
- `guideId`
- `quantity`
- `unitCost`

### Reglas

- La cantidad debe ser mayor a 0.
- El costo unitario debe ser mayor a 0.
- Pertenece a un solo pedido.
- Hace referencia a un solo tipo de guía.

***

## Account

Representa un lugar donde se encuentra el dinero del centro.

### Datos

- `id`
- `name`
- `balance`

### Reglas

- Inicialmente existen dos cuentas>
  - Efectivo
  - QR
- El saldo se calcula a partir de sus movimientos.
- Pueden agregarse nuevas cuentas en el futuro.

***

## AccountMovement

Representa una entrada o salida de dinero de una cuenta.

### Datos

- `id`
- `accountId`
- `type`
- `amount`
- `description`
- `date`

### Reglas

- El monto debe ser mayor a 0.
- Todo movimiento pertenece a una cuenta.
- `INCOME` aumenta el saldo.
- `EXPENSE` disminuye el saldo.
- Una venta genera un ingreso en Efectivo o QR.
- Un gasto o pago al proveedor genera una salida.

***

## CashClosure

Representa un cierre de caja realizado al finalizar un periodo de trabajo.

### Datos

- `id`
- `date`
- `expectedCash`
- `countedCash`
- `expectedQr`
- `reportedQr`
- `CashDifference`
- `qrDifference`
- `notes`

### Reglas

- Compara el dinero esperado con el dinero realmente contado/reportado.
- El efectivo y QR se verifican por separado.
- Una diferencia puede representar faltante o sobrante.
- El cierre no elimina ni modifica ventas anteriores.
- Un cierre queda guardado como historial.

***

## CashClosureItem

Representa el conteo físico de un tipo de guía durante un cierre.

### Datos 

- `id`
- `cashClosureId`
- `guideId`
- `expectedStock`
- `countedStock`
- `difference`

### Reglas

- Cada cierre contiene un registro por cada tipo de guía
- `expectedStock` es el stock calculado por el sistema.
- `countedStock` es lo que el encargado cuenta físicamente.
- La diferencia permite detectar pérdidas, sobrantes o ventas mal registradas.

