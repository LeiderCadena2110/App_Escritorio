# Arquitectura y estructura de carpetas

StockFlow sigue una arquitectura **por capas**. Cada capa solo conoce a la de abajo:

```
 Interfaz (JavaFX: vistas FXML + controladores)      ui/
        │
        ▼
 Servicios (reglas de negocio, transacciones)         service/
        │
        ▼
 Repositorios (SQL con Spring JDBC)                   repository/
        │
        ▼
 Base de datos local H2 (~/.stockflow/data)           db/migration/ (Flyway)
```

- Los **modelos** (`model/`) son las clases que viajan entre capas: `Product`, `Invoice`…
- Los **DTO** (`dto/`) son registros de solo lectura para reportes: `SalesSummary`, `TopProduct`…
- Spring Boot crea todos los objetos e inyecta las dependencias por constructor. No se levanta
  ningún servidor web (`spring.main.web-application-type=none`).

## Estructura de carpetas

```
App_Escritorio/
├── .github/workflows/          # CI: pruebas y generación de instaladores
│   ├── build-installers.yml
│   └── tests.yml
├── docs/                       # Documentación del proyecto
│   ├── architecture.md         # (este archivo)
│   ├── proposal.md             # Propuesta y reglas de negocio
│   ├── user-guide.md           # Manual de uso
│   └── screenshots/            # Capturas de pantalla
├── packaging/                  # Instaladores con jpackage
│   ├── icons/                  # app-icon.ico / .icns / .png
│   ├── build-linux.sh
│   ├── build-macos.sh
│   └── build-windows.bat
├── samples/                    # Ejemplos de archivos que genera la app
│   ├── products-export.csv
│   └── sample-invoice.pdf
├── src/
│   ├── main/
│   │   ├── java/co/edu/ucc/stockflow/
│   │   │   ├── Launcher.java               # main(): arranca JavaFX
│   │   │   ├── DesktopApp.java             # Pantalla de carga + Spring Boot
│   │   │   ├── StockFlowApplication.java   # Configuración de Spring Boot
│   │   │   ├── model/                      # Entidades del negocio
│   │   │   ├── dto/                        # Registros para reportes
│   │   │   ├── exception/                  # Errores de negocio
│   │   │   ├── repository/                 # Acceso a datos (SQL)
│   │   │   ├── service/                    # Reglas de negocio
│   │   │   ├── util/                       # Dinero, fechas y formatos
│   │   │   └── ui/                         # Interfaz gráfica
│   │   │       ├── MainController.java     # Ventana principal y menú
│   │   │       ├── Navigator.java          # Navegación entre secciones
│   │   │       ├── ViewLoader.java         # Carga de vistas FXML
│   │   │       ├── ThemeManager.java       # Tema claro / oscuro
│   │   │       ├── Dialogs.java            # Mensajes y confirmaciones
│   │   │       ├── component/              # Ayudas para tablas, formularios y fechas
│   │   │       ├── controller/             # Un controlador por sección
│   │   │       └── dialog/                 # Formularios emergentes
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── db/migration/               # Scripts SQL de Flyway
│   │       ├── icons/                      # Íconos de la ventana
│   │       ├── styles/                     # main.css y dark.css
│   │       └── views/                      # Una vista .fxml por sección
│   └── test/java/co/edu/ucc/stockflow/     # Pruebas JUnit 5
├── .editorconfig
├── .gitignore
├── pom.xml
└── README.md
```

## Convenciones

- **Nombres en inglés** para carpetas, archivos, paquetes, clases, métodos y columnas de la base
  de datos. Los textos que ve el usuario están en español.
- Archivos de recursos en minúscula y con guiones (`point-of-sale.fxml`, `app-icon-64.png`).
- Clases en `PascalCase`, métodos y variables en `camelCase`, constantes en `UPPER_SNAKE_CASE`,
  tablas y columnas en `snake_case` y en plural (`invoice_items`).
- Una vista FXML por sección, con su controlador en `ui/controller/` y el mismo nombre
  (`products.fxml` ↔ `ProductsController`).
- El stock **solo** cambia en `StockService`, y cada cambio deja un registro en `stock_movements`.

## Modelo de datos

```
categories 1───* products *───1 suppliers
                     │
                     ├──* stock_movements      (historial / kardex)
                     │
customers 1───* invoices 1───* invoice_items *───1 products

settings (clave / valor: datos del negocio, IVA, consecutivo, tema)
```

## Flujo de una venta

1. `PointOfSaleController` arma el carrito con `InvoiceItem` y muestra los totales que calcula
   `InvoiceService.calculateTotals`.
2. Al cobrar, `InvoiceService.issue`:
   - revisa que haya stock de todos los productos **antes** de guardar nada;
   - calcula subtotal, descuento, IVA y total; verifica el pago en efectivo;
   - toma el siguiente consecutivo (`SettingsService`) y guarda la factura con sus líneas;
   - llama a `StockService.register(..., SALE, ...)` por cada línea.
3. Todo ocurre en una sola transacción (`@Transactional`): si algo falla, no se guarda nada.
4. Anular la factura (`InvoiceService.cancel`) devuelve las unidades con movimientos
   `SALE_CANCELLATION` y marca la factura como `CANCELLED`.
