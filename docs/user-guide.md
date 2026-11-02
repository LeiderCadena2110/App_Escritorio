# Manual de uso de StockFlow

La primera vez que se abre, StockFlow trae un **catálogo de ejemplo** de una tienda de calzado
(13 productos, 5 categorías, 3 proveedores y 3 clientes) para que pueda probar la app de una vez.
Puede editar o eliminar esos datos cuando quiera.

## 1. Configurar el negocio

**Configuración** (Ctrl + 9):

- Escriba el nombre, NIT, dirección y teléfono del negocio: salen en las facturas.
- Revise el **IVA** (19 % por defecto; escriba 0 si no cobra IVA).
- El **prefijo** y el **siguiente número** definen el consecutivo: `FV-000001`, `FV-000002`…
- Elija el tema claro u oscuro.

## 2. Crear productos

**Productos** › *Nuevo producto*:

| Campo | Ejemplo | Nota |
|---|---|---|
| Código (SKU) | `DEP-004` | Único; se puede buscar por él en la venta |
| Precio de costo | `98.000` | Lo que le cuesta a usted |
| Precio de venta | `159.900` | Sin IVA. El margen se calcula solo |
| Stock inicial | `12` | Queda como movimiento "Inventario inicial" |
| Stock mínimo | `4` | Por debajo de este valor el producto sale como **stock bajo** |

Los botones *Categorías* y *Exportar CSV* están arriba. Con doble clic se edita un producto.

## 3. Registrar compras y ajustes

**Inventario** › *Entrada por compra*: elija el producto, la cantidad y el costo unitario. El
costo del producto se actualiza con el **promedio ponderado**.

*Ajuste / salida* sirve para:

- **Ajuste de inventario:** después de contar; use números negativos para restar.
- **Pérdida o daño**, **devolución a proveedor** o **devolución de cliente**.

El historial muestra cada movimiento con el stock que quedó. Desde *Productos › Ver historial*
se abre el historial de un solo producto.

## 4. Vender

**Vender** (Ctrl + 2, o Ctrl + N):

1. Escriba el código o el nombre y pulse **Enter** (o doble clic en la lista). Si el producto ya
   está en la venta, suma una unidad.
2. Ajuste cantidades con **+1 / −1**, *Cambiar cantidad…* o las teclas `+`, `−` y `Supr`.
3. Elija el cliente (por defecto, *Consumidor final*) o cree uno con **+ Nuevo**.
4. Elija la forma de pago. En efectivo, escriba el dinero recibido para ver el cambio.
5. Pulse **Cobrar y facturar** (F12). Puede guardar la factura en PDF de inmediato.

La app no deja vender más unidades de las que hay en stock.

## 5. Consultar y anular facturas

**Facturas**: filtre por fechas, número, cliente o estado. Con doble clic se ve el detalle, desde
donde se exporta a PDF o se **anula** (pide un motivo y devuelve las unidades al inventario).

## 6. Reportes

**Reportes**: elija un periodo (hoy, últimos 7 días, este mes, mes anterior o un rango) y vea:

- total vendido, número de facturas e IVA cobrado;
- ganancia bruta y margen (ventas sin IVA − costo de lo vendido);
- ticket promedio y unidades vendidas;
- ventas por día y productos más vendidos (exportables a CSV).

## Atajos de teclado

| Atajo | Acción |
|---|---|
| Ctrl + 1 … 9 | Cambiar de sección |
| Ctrl + N | Nueva venta |
| F2 | Ir al buscador en *Vender* |
| F12 | Cobrar y facturar |
| Ctrl + , | Configuración |
| Ctrl + Q | Salir |

En macOS use ⌘ en lugar de Ctrl.

## Copia de seguridad

Los datos están en la carpeta `.stockflow` de su usuario
(`C:\Users\<usuario>\.stockflow` en Windows, `/Users/<usuario>/.stockflow` en macOS y
`/home/<usuario>/.stockflow` en Linux). Cierre StockFlow y copie esa carpeta.
