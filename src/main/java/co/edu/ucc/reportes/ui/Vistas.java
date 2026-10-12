package co.edu.ucc.reportes.ui;

import co.edu.ucc.reportes.i18n.Textos;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/** Carga las vistas .fxml. Los controladores los crea Spring (setControllerFactory). */
@Component
public class Vistas {

    /** Vista cargada: el nodo y su controlador. */
    public record Cargada<T>(Parent raiz, T controlador) {
    }

    private final ApplicationContext contexto;
    private final Tema tema;
    private HostServices hostServices;

    public Vistas(ApplicationContext contexto, Tema tema) {
        this.contexto = contexto;
        this.tema = tema;
    }

    public <T> Cargada<T> cargar(String nombre) {
        URL url = Vistas.class.getResource("/fxml/" + nombre + ".fxml");
        FXMLLoader loader = new FXMLLoader(url, Textos.paquete());
        loader.setControllerFactory(contexto::getBean);
        try {
            Parent raiz = loader.load();
            return new Cargada<>(raiz, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar " + nombre + ".fxml", e);
        }
    }

    public void abrirVentanaPrincipal(Stage escenario) {
        Cargada<PrincipalController> principal = cargar("principal");
        Scene escena = new Scene(principal.raiz());
        tema.aplicar(escena);
        principal.controlador().iniciar(escenario, escena);
    }

    public HostServices hostServices() {
        return hostServices;
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }
}
