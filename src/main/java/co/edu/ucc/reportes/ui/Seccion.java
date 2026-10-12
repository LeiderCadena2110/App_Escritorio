package co.edu.ucc.reportes.ui;

/** Las 6 secciones de la barra lateral (Cmd/Ctrl + 1…6). */
public enum Seccion {
    INICIO("inicio"),
    DATOS("datos"),
    PLANTILLAS("plantillas"),
    GENERAR("generar"),
    HISTORIAL("historial"),
    CONFIGURACION("ajustes");

    private final String fxml;

    Seccion(String fxml) {
        this.fxml = fxml;
    }

    public String fxml() {
        return fxml;
    }

    public String claveTexto() {
        return "seccion." + name().toLowerCase();
    }
}
