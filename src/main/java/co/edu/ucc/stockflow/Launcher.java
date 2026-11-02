package co.edu.ucc.stockflow;

import javafx.application.Application;

/**
 * Entry point. It lives in a class that does NOT extend Application so JavaFX starts correctly
 * from the classpath (IntelliJ, "mvn spring-boot:run" and the installers).
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(DesktopApp.class, args);
    }
}
