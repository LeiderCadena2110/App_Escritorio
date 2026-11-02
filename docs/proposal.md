# Propuesta: StockFlow, inventario y facturación de escritorio

## Problema

La versión anterior del proyecto (Automatizador de Reportes) convertía archivos de Excel o CSV en
documentos Word y PDF. Era útil, pero no resolvía la necesidad de un negocio pequeño de saber
**qué tiene, qué vende y cuánto gana**.

Un local como una tienda de zapatos, una papelería o una miscelánea suele llevar el inventario en
cuadernos u hojas de cálculo. Por eso:

- no sabe a tiempo qué productos se están agotando;
- no lleva el historial de entradas y salidas de mercancía;
- las facturas se hacen a mano y los totales con IVA se calculan con calculadora;
- al final del mes no sabe cuánto vendió ni cuál fue su producto estrella.

## Solución

**StockFlow** es una aplicación de escritorio para Windows, macOS y Linux que funciona **sin
internet**. Guarda la información en una base de datos local (H2), dentro de la carpeta del usuario.

### Módulos

| Módulo | Qué hace |
|---|---|
| Panel principal (*Dashboard*) | Ventas del día y del mes, número de facturas, valor del inventario, productos con poco stock y los más vendidos |
| Productos | Crear, editar, buscar y desactivar productos (código o SKU, categoría, precio de costo y de venta, stock y stock mínimo) |
| Categorías | Agrupa los productos (por ejemplo: *Deportivos*, *Formales*, *Accesorios*) |
| Clientes | Datos de facturación: documento, nombre, teléfono, correo y dirección |
| Proveedores | A quién se le compra la mercancía |
| Inventario | Entradas por compra, salidas y ajustes, con el historial de cada movimiento |
| Punto de venta | Arma la factura: busca productos, controla cantidades, descuento, IVA y forma de pago |
| Facturas | Lista, detalle, exportación a PDF y anulación (al anular, el stock vuelve) |
| Reportes | Ventas por rango de fechas, productos más vendidos y exportación a CSV |
| Configuración | Datos del negocio (nombre, NIT, dirección, teléfono), % de IVA, prefijo de factura y tema claro u oscuro |

### Reglas de negocio

1. No se puede vender más unidades de las que hay en stock.
2. Cada venta descuenta el stock y deja un movimiento de inventario de tipo **SALE**.
3. Anular una factura devuelve las unidades al inventario con un movimiento **SALE_CANCELLATION**.
4. Los números de factura son consecutivos y llevan prefijo (por ejemplo, `FV-000001`).
5. Totales: `subtotal − descuento = base`; `IVA = base × % IVA`; `total = base + IVA`.
6. Un producto con stock menor o igual a su stock mínimo aparece como **stock bajo**.
7. Los productos no se borran si ya tienen ventas; se desactivan para conservar el historial.

## Tecnología

Se conserva la tecnología elegida para el proyecto de escritorio:

- **Java 21 LTS** y **JavaFX 21** para la interfaz (vistas FXML y hojas de estilo CSS).
- **Spring Boot 3** como contenedor de dependencias y servicios, sin servidor web.
- **Spring JDBC** con consultas SQL explícitas (patrón *Repository*).
- **H2** como base de datos local y **Flyway** para las migraciones.
- **Apache PDFBox** para generar la factura en PDF.
- **JUnit 5** para las pruebas.
- **jpackage** para los instaladores de Windows, macOS y Linux.
