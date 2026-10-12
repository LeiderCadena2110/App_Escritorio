package co.edu.ucc.reportes;

import javafx.application.Application;

/**
 * Punto de entrada. Va en una clase que NO extiende Application para que JavaFX
 * arranque bien desde el classpath (IntelliJ, "mvn spring-boot:run" y el instalador).
 */
public final class Lanzador {

    private Lanzador() {
    }

    public static void main(String[] args) {
        Application.launch(ReportesApp.class, args);
    }
}
