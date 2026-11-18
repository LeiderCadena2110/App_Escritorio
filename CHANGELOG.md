# Cambios

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).

## [Sin publicar] · Paso 3: modelo de dominio

### Agregado
- **IVA por producto** (19 %, 5 % o exento) y **régimen de IVA** del negocio (responsable o no responsable).
- Facturas con IVA desglosado por tarifa en el detalle y en el PDF.
- `TaxPolicy` y `PaymentPolicy` (patrón Strategy) para el IVA y las formas de pago.
- Las ventas a crédito exigen un cliente registrado (no "Consumidor final").

### Cambiado
- `Invoice` es un agregado: maneja sus líneas, aplica la regla de stock y calcula sus totales.
- La regla de stock está solo en `Product.checkCanTakeOut` (antes estaba en tres clases).
- El carrito (`SaleViewModel`) delega en el agregado en lugar de repetir las reglas.
- El descuento se reparte entre tarifas y el IVA se redondea al peso.

### Base de datos
- `V6`: `products.tax_rate` e `invoice_items.tax_rate`; `invoices.tax_rate` pasa a ser opcional.

## [Sin publicar] · Paso 2: bases para producción

### Agregado
- `Clock` inyectado: los servicios y repositorios ya no llaman `now()` directamente; las pruebas pueden fijar la fecha.
- Logging con SLF4J: arranque, ventas, anulaciones y movimientos quedan en `~/.stockflow/app.log`.
- Los errores inesperados muestran un **código de error** que también queda en el log.
- `ErrorCode` + `i18n/messages.properties`: las reglas de negocio lanzan códigos; el texto vive en un archivo de mensajes.
- Menú *Ayuda › Cargar datos de ejemplo* (solo con el catálogo vacío).

### Cambiado
- Los formularios guardan en segundo plano y muestran los errores de validación dentro del mismo formulario.
- El catálogo de ejemplo ya no se carga solo: requiere `stockflow.sample-data=true` o el menú *Ayuda*.
- `FileChoosers` pasa a ser un componente de Spring (sin estado estático).
- Los mensajes largos de los diálogos se ajustan en varias líneas.

### Corregido
- Eliminar un producto con compras o ajustes borraba su historial de inventario; ahora se desactiva.

## [1.1.0] · Fase 1: estructura y base de datos

### Agregado
- Código de barras en los productos (opcional y único). El punto de venta acepta lectores USB.
- Códigos de barras EAN-13 válidos en el catálogo de ejemplo.
- `SaleViewModel`: el carrito sigue el patrón MVVM y tiene sus propias pruebas.
- `TaskRunner`: consultas y exportaciones en segundo plano con indicador "Cargando…".
- Tabla `sequences` y `SequenceService` para el consecutivo de facturas.

### Cambiado
- El consecutivo de facturas se reserva bloqueando su fila, así nunca se repite.
- Inicio, Productos, Inventario, Facturas, Reportes y Vender ya no congelan la ventana al cargar.
- No se puede cobrar dos veces la misma venta pulsando el botón (o F12) repetido.

### Base de datos
- `V2`: índices para facturas, productos y movimientos.
- `V3`: restricciones `CHECK` (precios ≥ 0, descuento ≤ subtotal, estados y tipos válidos).
- `V4`: columna `barcode` con índice único.
- `V5`: tabla `sequences`; conserva el prefijo y el número que ya tenía cada usuario.

## [1.0.0] · Primera versión de StockFlow

- Inventario, punto de venta, facturas en PDF, clientes, proveedores, reportes y configuración.
