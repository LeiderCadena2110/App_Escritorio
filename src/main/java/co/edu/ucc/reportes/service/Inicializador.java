package co.edu.ucc.reportes.service;

import java.nio.file.Files;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Al arrancar: carpetas de la app y plantillas HTML prediseñadas. */
@Component
public class Inicializador implements ApplicationRunner {

    private final ServicioPlantillas plantillas;

    public Inicializador(ServicioPlantillas plantillas) {
        this.plantillas = plantillas;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Files.createDirectories(ServicioPlantillas.carpetaPlantillas());
        plantillas.asegurarIntegradas();
    }
}
