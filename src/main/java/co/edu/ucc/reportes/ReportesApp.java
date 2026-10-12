package co.edu.ucc.reportes;

import co.edu.ucc.reportes.i18n.Textos;
import co.edu.ucc.reportes.service.ServicioAjustes;
import co.edu.ucc.reportes.ui.Vistas;
import java.io.InputStream;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Arranca JavaFX, muestra "Iniciando…" mientras Spring Boot prepara servicios y base local,
 * y luego abre la ventana principal.
 */
public class ReportesApp extends Application {

    private ConfigurableApplicationContext contexto;

    @Override
    public void start(Stage escenario) {
        escenario.setTitle("Automatizador de Reportes");
        for (String tam : new String[]{"16", "32", "64", "256"}) {
            InputStream in = getClass().getResourceAsStream("/iconos/icono-" + tam + ".png");
            if (in != null) escenario.getIcons().add(new Image(in));
        }
        escenario.setScene(pantallaDeCarga());
        escenario.show();

        String[] args = getParameters().getRaw().toArray(String[]::new);
        Task<ConfigurableApplicationContext> arranque = new Task<>() {
            @Override
            protected ConfigurableApplicationContext call() {
                // headless(false): permite abrir carpetas con el explorador del sistema
                return new SpringApplicationBuilder(AplicacionSpring.class).headless(false).run(args);
            }
        };
        arranque.setOnSucceeded(e -> {
            contexto = arranque.getValue();
            Textos.iniciar(contexto.getBean(ServicioAjustes.class).idioma());
            Vistas vistas = contexto.getBean(Vistas.class);
            vistas.setHostServices(getHostServices());
            vistas.abrirVentanaPrincipal(escenario);
        });
        arranque.setOnFailed(e -> {
            Throwable t = arranque.getException();
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Automatizador de Reportes");
            alerta.setHeaderText(Textos.t("arranque.error"));
            alerta.setContentText(causaRaiz(t));
            alerta.showAndWait();
            Platform.exit();
        });
        Thread hilo = new Thread(arranque, "arranque-spring");
        hilo.setDaemon(true);
        hilo.start();
    }

    @Override
    public void stop() {
        if (contexto != null) contexto.close();
    }

    private Scene pantallaDeCarga() {
        Label titulo = new Label("Automatizador de Reportes");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label sub = new Label("Iniciando… / Starting…");
        ProgressIndicator p = new ProgressIndicator();
        p.setPrefSize(42, 42);
        VBox caja = new VBox(14, titulo, p, sub);
        caja.setAlignment(Pos.CENTER);
        return new Scene(caja, 420, 240);
    }

    private static String causaRaiz(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) c = c.getCause();
        String m = c.getMessage() == null ? c.getClass().getSimpleName() : c.getMessage();
        if (m.contains("Database may be already in use") || m.contains("already in use")) {
            return Textos.t("arranque.yaAbierta");
        }
        return m;
    }
}
