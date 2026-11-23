# Manual de uso de StockFlow

La primera vez que se abre, StockFlow trae un **catálogo de ejemplo** de una tienda de calzado
(13 productos, 5 categorías, 3 proveedores y 3 clientes) para que pueda probar la app de una vez.
Puede editar o eliminar esos datos cuando quiera.

## 1. Configurar el negocio

**Configuración** (Ctrl + 9):

- Escriba el nombre, NIT, dirección y teléfono del negocio: salen en las facturas.
- Elija el **régimen de IVA**: *Responsable de IVA* (cada factura cobra el IVA de cada producto) o
  *No responsable de IVA* (las facturas no cobran IVA).
- El **IVA de productos nuevos** (19 % por defecto) es la tarifa que se propone al crear un producto;
  cada producto puede tener la suya.
- El **prefijo** y el **siguiente número** definen el consecutivo: `FV-000001`, `FV-000002`…
- Elija el tema claro u oscuro.

## 2. Crear productos

**Productos** › *Nuevo producto*:

| Campo | Ejemplo | Nota |
|---|---|---|
| Código (SKU) | `DEP-004` | Único; se puede buscar por él en la venta |
| Código de barras | `7709000000012` | Opcional y único; permite vender con lector |
| Precio de costo | `98.000` | Lo que le cuesta a usted |
| Precio de venta | `159.900` | Sin IVA. El margen se calcula solo |
| IVA | `19 %` | 19 %, 5 % o 0 % (exento o excluido); se suma en la factura |
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

1. Escriba el código o el nombre y pulse **Enter** (o doble clic en la lista). No importan las
   tildes ni el orden de las palabras: "zapatilla nike" encuentra "Nike Zapatilla". Con un **lector de
   código de barras** USB basta con escanear: el lector escribe el código y pulsa Enter solo. Si el
   producto ya está en la venta, suma una unidad.
2. Ajuste cantidades con **+1 / −1**, *Cambiar cantidad…* o las teclas `+`, `−` y `Supr`.
3. Cliente (F3): escriba parte del nombre, del documento o del teléfono y elíjalo de la lista con
   **Enter** o un clic. Por defecto es *Consumidor final*; **Nuevo** crea un cliente sin salir de la venta.
4. Elija la forma de pago. En efectivo, pulse uno de los **botones de pago rápido** (*Exacto* o el
   billete con que le pagan, por ejemplo *$ 50.000*) o escriba el valor recibido: el cambio aparece al instante.
5. Pulse **Cobrar y facturar** (F12). Abajo a la derecha aparece un aviso con el **cambio** y el
   botón **Guardar PDF**; el aviso se cierra solo y puede atender al siguiente cliente de una vez.

La app no deja vender más unidades de las que hay en stock. Si intenta cerrar StockFlow con una
venta sin cobrar, le pregunta antes para que no se pierda.

## 5. Consultar y anular facturas

**Facturas**: filtre por fechas, número, cliente o estado. Con doble clic se ve el detalle, desde
donde se exporta a PDF o se **anula** (pide un motivo y devuelve las unidades al inventario).

## 6. Reportes

**Reportes**: elija un periodo (hoy, últimos 7 días, este mes, mes anterior o un rango) y vea:

- total vendido, número de facturas e IVA cobrado;
- ganancia bruta y margen (ventas sin IVA − costo de lo vendido);
- ticket promedio y unidades vendidas;
- ventas por día y productos más vendidos (exportables a CSV).

## 7. Asistente IA

**Asistente IA** (Ctrl + 9) ayuda con las compras. La primera vez pulse **Configurar**, cree una
clave gratuita en [Google AI Studio](https://aistudio.google.com/apikey) con su cuenta de Google,
péguela y pulse **Guardar y probar**.

- **Leer factura de proveedor:** pulse **Elegir foto o PDF…** y escoja la factura. En unos segundos
  aparece cada línea con el producto que encontró StockFlow (*Por código*, *Por nombre* o *Elegir
  producto*), la cantidad y el costo sin IVA. Corrija lo necesario, desmarque lo que no quiera
  sumar y pulse **Registrar compra**: el stock y el costo promedio se actualizan con la referencia de la factura.
- **¿Qué debo comprar?** La tabla se calcula sola (no necesita clave) con el stock, el mínimo y lo
  vendido en los últimos 15, 30, 60 o 90 días: prioridad, cuántos días alcanza y cuántas unidades
  comprar. Con **Nota de compra con IA** obtiene un pedido redactado por proveedor para copiar y
  enviar por WhatsApp o correo. Doble clic en un producto abre su historial en *Inventario*.

La IA puede equivocarse al leer: revise siempre antes de registrar. Solo se envían a Google las
facturas que usted elija y los nombres y cantidades del plan; nunca datos de sus clientes.

## Atajos de teclado

| Atajo | Acción |
|---|---|
| Ctrl + 1 … 9 y 0 | Cambiar de sección (0 = Configuración) |
| Ctrl + N | Nueva venta |
| F2 | Ir al buscador en *Vender* |
| F3 | Ir al cliente en *Vender* |
| F12 | Cobrar y facturar |
| Ctrl + , | Configuración |
| Ctrl + Q | Salir |

En macOS use ⌘ en lugar de Ctrl.

## Copia de seguridad

Los datos están en la carpeta `.stockflow` de su usuario
(`C:\Users\<usuario>\.stockflow` en Windows, `/Users/<usuario>/.stockflow` en macOS y
`/home/<usuario>/.stockflow` en Linux). Cierre StockFlow y copie esa carpeta.
