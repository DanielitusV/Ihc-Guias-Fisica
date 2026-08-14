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
