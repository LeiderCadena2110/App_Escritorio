# Arquitectura y estructura de carpetas

StockFlow es un **monolito modular**: un solo programa dividido en módulos por funcionalidad
(catálogo, inventario, ventas…). Cada módulo sigue la arquitectura de **puertos y adaptadores**
(hexagonal) con cuatro capas:

```
            ui/  (JavaFX: controladores, diálogos, view models)
             │ usa
             ▼
   application/  (servicios / casos de uso, transacciones)
             │ usa                         ▲ implementa los puertos
             ▼                             │
        domain/  (entidades, reglas,  ◄── infrastructure/  (JDBC, PDF)
                  puertos = interfaces)
```

- **domain**: Java puro. Entidades con sus reglas (`Product.checkCanTakeOut`, el agregado `Invoice`),
  estrategias (`TaxPolicy`, `PaymentPolicy`) y **puertos**: interfaces como `ProductRepository`.
  No conoce Spring, JavaFX ni SQL.
- **application**: casos de uso (`InvoiceService.issue`, `StockService.register`…). Depende de los
  puertos, nunca de su implementación.
- **infrastructure**: **adaptadores** que implementan los puertos: `JdbcProductRepository` (SQL con
  Spring JDBC) o `PdfInvoiceExporter` (PDFBox). Se pueden cambiar sin tocar las reglas: pasar de H2
  a PostgreSQL solo afectaría esta capa.
- **ui**: pantallas JavaFX. Usan servicios, nunca repositorios.

Spring Boot crea todos los objetos e inyecta las dependencias por constructor. No se levanta ningún
servidor web (`spring.main.web-application-type=none`).

## Módulos y sus dependencias

| Módulo | Qué contiene | Puede usar |
|---|---|---|
| `shared` | Errores de negocio (`ErrorCode`), dinero y fechas, `JdbcHelper`, componentes de interfaz | — |
| `settings` | Datos del negocio, régimen de IVA, consecutivos, tema | shared |
| `catalog` | Productos, categorías, proveedores | shared, settings |
| `customers` | Clientes | shared |
| `inventory` | Movimientos de stock (kardex) | shared, catalog |
| `sales` | Facturas, IVA, formas de pago, punto de venta, PDF | shared, settings, catalog, customers, inventory |
| `reporting` | Panel de inicio y reportes (solo lectura) | todos los anteriores |
| `app` | Ventana principal, menú, datos de ejemplo | todos |

Las dependencias solo van "hacia abajo", así que no hay ciclos. La **interfaz** sí puede abrir
pantallas de otros módulos (por ejemplo, *Productos* abre el diálogo de movimientos de *Inventario*),
porque es la capa que los combina.

Dos técnicas evitan dependencias en sentido contrario:

- **Evento de dominio `ProductCreated`.** El catálogo lo publica al crear un producto con unidades y
  el inventario lo escucha (`InitialStockListener`) para registrar el inventario inicial. Así el
  catálogo no depende del inventario.
- **`Navigator.open(sección, dato)`.** Una pantalla abre otra pasándole un dato (por ejemplo, un
  producto), sin conocer la clase de su controlador.

### Reglas verificadas automáticamente

`ArchitectureTest` revisa el código en cada `mvn test` (y en GitHub Actions) y hace fallar el build si:

1. una clase de `domain` usa Spring, JavaFX o SQL, o depende de otra capa;
2. `application` usa JavaFX, `ui` o `infrastructure`;
3. `infrastructure` usa JavaFX o `ui`;
4. `ui` usa `infrastructure` (por ejemplo, un repositorio directamente);
5. un módulo usa otro que no está en la tabla de arriba;
6. `shared` o un módulo usa `app`.

## Estructura de carpetas

```
App_Escritorio/
├── .github/workflows/          # CI: pruebas y generación de instaladores
├── docs/                       # Documentación (propuesta, arquitectura, manual, capturas)
├── packaging/                  # Instaladores con jpackage e íconos
├── samples/                    # Ejemplos de archivos que genera la app (PDF, CSV)
├── src/
│   ├── main/
│   │   ├── java/co/edu/ucc/stockflow/
│   │   │   ├── Launcher.java · DesktopApp.java · StockFlowApplication.java   # arranque
│   │   │   ├── app/                 # MainController, MainWindow, DataSeeder
│   │   │   ├── shared/
│   │   │   │   ├── domain/          # BusinessException, ErrorCode, Messages
│   │   │   │   ├── util/            # Money, DateRange, Formats, CsvWriter
│   │   │   │   ├── config/          # TimeConfig (Clock)
│   │   │   │   ├── persistence/     # JdbcHelper
│   │   │   │   └── ui/              # Dialogs, TaskRunner, Navigator, ThemeManager, component/, dialog/
│   │   │   ├── settings/            # domain/ application/ infrastructure/ ui/
│   │   │   ├── catalog/             # domain/ application/ infrastructure/ ui/
│   │   │   ├── customers/           # domain/ application/ infrastructure/ ui/
│   │   │   ├── inventory/           # domain/ application/ infrastructure/ ui/
│   │   │   ├── sales/               # domain/ (tax/, payment/) application/ (payment/) infrastructure/ ui/
│   │   │   └── reporting/           # application/ infrastructure/ ui/
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── db/migration/        # Scripts SQL de Flyway (V1…V6)
│   │       ├── i18n/                # messages.properties (textos de los errores)
│   │       ├── icons/ · styles/     # Íconos de la ventana, main.css y dark.css
│   │       └── views/               # Una vista .fxml por sección
│   └── test/java/co/edu/ucc/stockflow/   # Pruebas en los mismos paquetes + ArchitectureTest
├── CHANGELOG.md · README.md · pom.xml · .editorconfig · .gitignore
```

Ejemplo completo de un módulo (`catalog`):

```
catalog/
├── domain/           Product, Category, Supplier, ProductCreated (evento)
│                     ProductRepository, CategoryRepository, SupplierRepository  ← puertos
├── application/      ProductService, CategoryService, SupplierService, ProductCsvExport
├── infrastructure/   JdbcProductRepository, JdbcCategoryRepository, JdbcSupplierRepository  ← adaptadores
└── ui/               ProductsController, SuppliersController, ProductDialog, CategoryManagerDialog, SupplierDialog
```

## Convenciones

- **Nombres en inglés** para carpetas, archivos, paquetes, clases, métodos y columnas de la base
  de datos. Los textos que ve el usuario están en español.
- Archivos de recursos en minúscula y con guiones (`point-of-sale.fxml`, `app-icon-64.png`).
- Clases en `PascalCase`, métodos y variables en `camelCase`, constantes en `UPPER_SNAKE_CASE`,
  tablas y columnas en `snake_case` y en plural (`invoice_items`).
- Una vista FXML por sección, con su controlador en el `ui/` de su módulo y el mismo nombre
  (`products.fxml` ↔ `catalog.ui.ProductsController`).
- Los puertos se llaman como lo que guardan (`ProductRepository`); sus adaptadores llevan la
  tecnología delante (`JdbcProductRepository`, `PdfInvoiceExporter`).
- El stock **solo** cambia en `StockService`, y cada cambio deja un registro en `stock_movements`.

## Modelo de datos

```
categories 1───* products *───1 suppliers
                     │
                     ├──* stock_movements      (historial / kardex)
                     │
customers 1───* invoices 1───* invoice_items *───1 products

settings  (clave / valor: datos del negocio, IVA, tema)
sequences (series de numeración: prefijo y siguiente número de las facturas)
```

### Migraciones (Flyway)

Cada cambio a la base de datos es un script nuevo en `db/migration/`; los anteriores **nunca se
editan**, así la base de cualquier usuario se actualiza sola al abrir la nueva versión.

| Script | Qué hace |
|---|---|
| `V1__initial_schema.sql` | Tablas iniciales |
| `V2__add_indexes.sql` | Índices para búsquedas y reportes |
| `V3__add_check_constraints.sql` | Reglas en la base: precios ≥ 0, estados y tipos válidos |
| `V4__add_product_barcode.sql` | Código de barras opcional y único |
| `V5__add_sequences.sql` | Tabla `sequences` para el consecutivo (copia el valor que había en `settings`) |
| `V6__add_tax_rate_per_product.sql` | IVA por producto y por línea de factura (conserva la tarifa configurada) |

## Modelo de dominio

Las reglas del negocio viven en el modelo, no en las pantallas ni repartidas entre servicios:

| Regla | Dónde vive | Quién la usa |
|---|---|---|
| No sacar más unidades de las que hay | `Product.checkCanTakeOut(int)` | Carrito, `InvoiceService`, `StockService` |
| Líneas, descuento y totales de una factura | Agregado `Invoice` (`addLine`, `changeQuantity`, `calculate`) | `SaleViewModel`, `InvoiceService` |
| Qué IVA paga cada línea | Estrategia `TaxPolicy` (`VatResponsiblePolicy`, `NotVatResponsiblePolicy`) | `Invoice.calculate` |
| Cómo se paga | Estrategia `PaymentPolicy` (efectivo, tarjeta, transferencia, crédito) + `PaymentPolicies` | `InvoiceService.issue` |

**Agregado `Invoice`.** Las líneas son de solo lectura desde afuera (`getItems()` no se puede
modificar); se cambian con sus métodos, que aplican la regla de stock. `calculate(TaxPolicy)`
calcula todo en un solo lugar:

1. cada línea toma la tarifa que le da la política (la del producto, o 0 % si el negocio es no
   responsable de IVA);
2. subtotal = suma de las líneas; el descuento se limita entre 0 y el subtotal;
3. el descuento se reparte entre las tarifas en proporción a su valor, **en pesos enteros**;
4. IVA de cada tarifa = base × tarifa / 100, redondeado al peso; total = subtotal − descuento + IVA.

**Estrategias (patrón Strategy).** Agregar una forma de pago (por ejemplo, pagos mixtos) es crear
una clase que implemente `PaymentPolicy` con `@Component`; `PaymentPolicies` la encuentra sola y
`InvoiceService` no cambia (principio abierto/cerrado).

## Interfaz: MVVM y segundo plano

- **MVVM en el punto de venta:** `SaleViewModel` guarda el carrito, el cliente, el pago y calcula
  los totales con propiedades de JavaFX. El controlador solo enlaza (*bind*) los controles a esas
  propiedades. Así las reglas del carrito se prueban con JUnit sin abrir ventanas
  (`SaleViewModelTest`).
- **Segundo plano:** las consultas y exportaciones pasan por `TaskRunner`, que las ejecuta en un
  hilo aparte (en orden) y devuelve el resultado al hilo de JavaFX. Mientras trabaja, la ventana
  muestra "Cargando…" y nunca se congela. Patrón usado en los controladores:

```java
tasks.run("No se pudieron cargar las facturas",
        () -> invoices.search(range, text, status),   // segundo plano: base de datos
        this::render);                                 // hilo de JavaFX: actualizar la tabla
```

## Flujo de una venta

1. `SaleViewModel` arma el carrito con `InvoiceItem`, revisa el stock y recalcula los totales
   (`InvoiceService.calculateTotals`) cada vez que algo cambia.
2. Al cobrar, `InvoiceService.issue`:
   - revisa que haya stock de todos los productos **antes** de guardar nada;
   - calcula subtotal, descuento, IVA y total; verifica el pago en efectivo;
   - toma el siguiente consecutivo (`SequenceService`, que bloquea la fila con `SELECT … FOR UPDATE`)
     y guarda la factura con sus líneas;
   - llama a `StockService.register(..., SALE, ...)` por cada línea.
3. Todo ocurre en una sola transacción (`@Transactional`): si algo falla, no se guarda nada.
4. Anular la factura (`InvoiceService.cancel`) devuelve las unidades con movimientos
   `SALE_CANCELLATION` y marca la factura como `CANCELLED`.
