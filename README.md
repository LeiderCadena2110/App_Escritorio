# Automatizador de Reportes

App de escritorio para **Windows, macOS y Linux** que toma un Excel o CSV, lo combina con una plantilla y genera los informes en **Word (.docx)** o **PDF**, sin escribirlos a mano. Todo se procesa en el computador: ningún dato sale a internet.

**Stack:** Java 21 · JavaFX 21 (FXML) · Spring Boot 3.5 sin servidor web · H2 en archivo · Flyway · Apache POI + poi-tl · Commons CSV · Thymeleaf + OpenHTMLtoPDF · JUnit 5 · jpackage

## Qué hace

| Sección | Qué permite |
|---|---|
| 1. Inicio | Nuevo reporte, reportes recientes y perfiles guardados |
| 2. Datos | Importar .xlsx/.csv (o arrastrarlo), elegir hoja, vista previa y revisión de datos (celdas vacías, texto en columnas de números, fechas mal escritas), con fila y columna |
| 3. Plantillas | Registrar plantillas Word, ver sus marcadores `{{campo}}`, abrirlas en Word; 3 plantillas PDF incluidas (certificado, cuenta de cobro, listado) |
| 4. Generar reporte | Asistente de 4 pasos: Datos › Plantilla › Mapeo › Salida, con progreso y botón Cancelar |
| 5. Historial | Cada generación con su resultado y errores; abrir la carpeta de salida |
| 6. Configuración | Carpeta de salida, formato de fecha y números, idioma (español/inglés), tema claro u oscuro |

**Atajos:** Ctrl+N nuevo reporte · Ctrl+O importar · Ctrl+1…6 secciones · Ctrl+, configuración · Enter siguiente · Alt+← atrás · Esc cancelar. En macOS se usa Cmd en lugar de Ctrl, y Cmd+[ para ir atrás.

## Cómo abrirlo y correrlo (Windows)

1. Descomprima el proyecto en una ruta **sin tildes ni ñ**, por ejemplo `C:\Proyecto_Escritorio\automatizador-reportes`.
2. Instale **IntelliJ IDEA Community** (gratis).
3. IntelliJ › *Open* › elija la carpeta `automatizador-reportes` (la que tiene `pom.xml`) › *Trust Project*.
4. Si pide un JDK: *File › Project Structure › SDK › Download JDK* › versión **21** (por ejemplo, Eclipse Temurin).
5. Espere a que Maven descargue las librerías (barra inferior). La primera vez tarda unos minutos.
6. Abra `src/main/java/co/edu/ucc/reportes/Lanzador.java` y pulse el ▶ verde junto a `main`.

También se puede correr desde la terminal con Maven: `mvn spring-boot:run`.

> Ejecute siempre **Lanzador**, no `ReportesApp`: si se ejecuta la clase que extiende `Application`, Java muestra "JavaFX runtime components are missing".

## Prueba rápida con los ejemplos

En la carpeta `ejemplos/`:

| Archivo | Para qué |
|---|---|
| `clientes.xlsx` | Hoja *Clientes* (6 clientes) y hoja *Productos* |
| `participantes.csv` | CSV con punto y coma y tildes, como lo guarda Excel en Windows |
| `plantilla-carta.docx` | Plantilla Word para **un informe por fila** |
| `plantilla-pedido.docx` | Plantilla Word con tabla `{{filas}}` para **un informe con todas las filas** |

1. **Certificados en PDF:** Generar reporte › elegir `participantes.csv` › *Certificado (PDF)* › en el mapeo, `fecha` = *Fecha de entrega* › Un informe por fila › Nombre de cada archivo = *Nombre* › Generar.
2. **Cartas en Word:** Plantillas › Registrar plantilla Word › `plantilla-carta.docx` › Generar reporte con `clientes.xlsx` (hoja *Clientes*) › en el mapeo, `fecha` = *(Fecha de hoy)*.
3. **Pedido con tabla:** registre `plantilla-pedido.docx` › datos `clientes.xlsx`, hoja *Productos* › modo *Un informe con todas las filas* › `cantidad_productos` = *(Total de filas)*.

## Cómo escribir una plantilla de Word

- Escriba los marcadores con doble llave donde va cada dato: `{{cliente}}`, `{{ciudad}}`, `{{total}}`. Pueden llevar tildes y espacios, pero no puntos.
- Escriba cada marcador de una sola vez, sin cambiarle el formato a la mitad (si Word lo parte, no se reconoce).
- **Tabla con todas las filas:** en la fila de títulos de la tabla agregue `{{filas}}` en cualquier celda, y en la fila de abajo escriba `[producto]`, `[cantidad]`, `[valor]`. Esa fila se repite por cada registro.
- Después de editar una plantilla ya registrada (botón *Abrir en Word*), pulse **Volver a leer marcadores**.

En el mapeo, además de las columnas del archivo, se puede elegir *(Fecha de hoy)*, *(Número de fila)* y *(Total de filas)*.

## Pruebas

```bash
mvn test
```

Prueban la lectura de CSV (separador y codificación), Excel (fechas, formatos y fórmulas), la revisión de datos, la detección de marcadores, la sugerencia de columnas, los nombres de archivo, el PDF de las 3 plantillas HTML y el Word por fila y con tabla.

## Instaladores (jpackage)

Cada instalador se arma en su propio sistema y lleva Java incluido. Primero ejecute `package` de Maven (IntelliJ: panel *Maven › Lifecycle › package*), que crea `target/app`.

| Sistema | Comando | Resultado |
|---|---|---|
| Windows | `empaquetado\empaquetar-windows.bat` | Carpeta portable con `Automatizador de Reportes.exe` |
| Windows | `empaquetado\empaquetar-windows.bat exe` | Instalador `.exe` (requiere WiX Toolset 3.14) |
| macOS | `empaquetado/empaquetar-macos.sh` | `.dmg` |
| Linux | `empaquetado/empaquetar-linux.sh` | `.deb` y `.AppImage` |

Si `jpackage` no se encuentra, defina `JAVA_HOME` con la carpeta del JDK 21 (IntelliJ los guarda en `C:\Users\<usuario>\.jdks\`).

**GitHub Actions** (`.github/workflows/instaladores.yml`) arma los cuatro en la nube: *Actions › Instaladores › Run workflow*, o automáticamente al publicar una etiqueta `v1.0.0`.

Sin firma digital (MVP): en Windows, SmartScreen › *Más información* › *Ejecutar de todas formas*. En macOS 15, *Ajustes del Sistema › Privacidad y seguridad › Abrir de todos modos*.

## Dónde guarda la app sus datos

`~/.automatizador-reportes/` (en Windows `C:\Users\<usuario>\.automatizador-reportes\`):

- `datos.mv.db`: base H2 con plantillas, perfiles, historial y ajustes.
- `plantillas/`: copia de cada plantilla Word registrada.
- `app.log`: registro para diagnosticar errores.

Los informes se guardan por defecto en `Documentos/Reportes generados/`, en una subcarpeta nueva por cada generación.

## Estructura

```
automatizador-reportes/
├── pom.xml
├── ejemplos/                         # datos y plantillas para probar
├── empaquetado/                      # scripts de jpackage e íconos
├── .github/workflows/                # instaladores en GitHub Actions
└── src/
    ├── main/java/co/edu/ucc/reportes/
    │   ├── Lanzador.java             # main: arranca JavaFX
    │   ├── ReportesApp.java          # pantalla "Iniciando…", arranca Spring y abre la ventana
    │   ├── AplicacionSpring.java     # @SpringBootApplication (sin web)
    │   ├── ui/                       # controladores de las 6 secciones y apoyo (Vistas, Tema, Navegador…)
    │   ├── service/                  # LectorCsv, LectorExcel, ValidadorDatos, DetectorMarcadores,
    │   │                             # SugeridorMapeo, GeneradorInforme, MotorPdf, servicios de datos
    │   ├── domain/                   # Plantilla, PerfilMapeo, Ejecucion, Ajuste
    │   ├── repository/               # Spring Data JPA
    │   └── i18n/                     # textos en español e inglés
    ├── main/resources/
    │   ├── fxml/                     # una vista por pantalla
    │   ├── css/                      # tema claro (base.css) y oscuro
    │   ├── plantillas-html/          # plantillas PDF (Thymeleaf)
    │   ├── db/migration/             # Flyway
    │   ├── i18n/                     # textos*.properties
    │   └── application.properties
    └── test/java/                    # JUnit 5
```

## Problemas comunes

| Mensaje | Solución |
|---|---|
| *JavaFX runtime components are missing* | Ejecute `Lanzador`, no `ReportesApp` |
| *Parece que el Automatizador ya está abierto* | Cierre la otra ventana: la base H2 solo admite una a la vez |
| *El archivo está abierto en otro programa* | Cierre el Excel o el Word y vuelva a intentar |
| Un marcador no se reemplaza en Word | Bórrelo y escríbalo de nuevo de una sola vez; luego *Volver a leer marcadores* |
| Ruta con tildes o ñ | Mueva el proyecto a una carpeta como `C:\Proyecto_Escritorio` |
