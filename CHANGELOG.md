# Cambios

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).

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
