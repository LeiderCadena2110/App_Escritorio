package co.edu.ucc.reportes.domain;

/** DOCX: plantilla de Word del usuario. HTML: plantilla prediseñada de la app (sale en PDF). */
public enum TipoPlantilla {
    DOCX("docx"),
    HTML("pdf");

    private final String extensionSalida;

    TipoPlantilla(String extensionSalida) {
        this.extensionSalida = extensionSalida;
    }

    public String extensionSalida() {
        return extensionSalida;
    }
}
