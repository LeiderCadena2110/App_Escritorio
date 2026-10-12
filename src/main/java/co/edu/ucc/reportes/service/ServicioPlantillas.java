package co.edu.ucc.reportes.service;

import co.edu.ucc.reportes.domain.Plantilla;
import co.edu.ucc.reportes.domain.TipoPlantilla;
import co.edu.ucc.reportes.i18n.Textos;
import co.edu.ucc.reportes.repository.PlantillaRepositorio;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Plantillas de Word registradas por el usuario y plantillas HTML prediseñadas para PDF.
 * Al registrar un .docx se guarda una copia en ~/.automatizador-reportes/plantillas/,
 * así la plantilla sigue funcionando aunque el archivo original se mueva.
 */
@Service
public class ServicioPlantillas {

    /** Plantillas HTML que trae la app: recurso, nombre y descripción. */
    static final String[][] INTEGRADAS = {
            {"certificado", "plantilla.integrada.certificado", "plantilla.integrada.certificado.desc"},
            {"cuenta-cobro", "plantilla.integrada.cuentaCobro", "plantilla.integrada.cuentaCobro.desc"},
            {"listado", "plantilla.integrada.listado", "plantilla.integrada.listado.desc"},
    };

    private final PlantillaRepositorio repo;

    public ServicioPlantillas(PlantillaRepositorio repo) {
        this.repo = repo;
    }

    public static Path carpetaPlantillas() {
        return Path.of(System.getProperty("user.home"), ".automatizador-reportes", "plantillas");
    }

    public List<Plantilla> listar() {
        return repo.findAllByOrderByIntegradaAscNombreAsc();
    }

    /** Crea en la base las plantillas HTML de la app que todavía no estén. */
    public void asegurarIntegradas() throws IOException {
        for (String[] p : INTEGRADAS) {
            if (repo.findFirstByTipoAndRuta(TipoPlantilla.HTML, p[0]).isPresent()) continue;
            Plantilla nueva = new Plantilla();
            nueva.setNombre(Textos.t(p[1]));
            nueva.setDescripcion(Textos.t(p[2]));
            nueva.setTipo(TipoPlantilla.HTML);
            nueva.setRuta(p[0]);
            nueva.setIntegrada(true);
            nueva.setCreada(LocalDateTime.now());
            aplicar(nueva, DetectorMarcadores.detectarHtml(leerHtml(p[0])));
            repo.save(nueva);
        }
    }

    public Plantilla registrarDocx(Path origen, String nombre) throws IOException {
        Marcadores m = DetectorMarcadores.detectarDocx(origen);
        Files.createDirectories(carpetaPlantillas());
        String archivo = UUID.randomUUID().toString().substring(0, 8) + "-"
                + NombresArchivo.limpiar(origen.getFileName().toString().replaceFirst("(?i)\\.docx$", ""), "plantilla") + ".docx";
        Path copia = carpetaPlantillas().resolve(archivo);
        Files.copy(origen, copia, StandardCopyOption.REPLACE_EXISTING);

        Plantilla p = new Plantilla();
        p.setNombre(nombre);
        p.setTipo(TipoPlantilla.DOCX);
        p.setRuta(copia.toString());
        p.setIntegrada(false);
        p.setCreada(LocalDateTime.now());
        p.setDescripcion(Textos.t("plantilla.registradaDesde", origen.toString()));
        aplicar(p, m);
        return repo.save(p);
    }

    /** Vuelve a leer los marcadores (después de editar la plantilla en Word). */
    public Plantilla volverALeer(Plantilla p) throws IOException {
        Marcadores m = p.getTipo() == TipoPlantilla.DOCX
                ? DetectorMarcadores.detectarDocx(Path.of(p.getRuta()))
                : DetectorMarcadores.detectarHtml(leerHtml(p.getRuta()));
        aplicar(p, m);
        return repo.save(p);
    }

    public void eliminar(Plantilla p) throws IOException {
        if (p.isIntegrada()) return;
        repo.delete(p);
        if (p.getTipo() == TipoPlantilla.DOCX) Files.deleteIfExists(Path.of(p.getRuta()));
    }

    public String leerHtml(String recurso) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/plantillas-html/" + recurso + ".html")) {
            if (in == null) throw new IOException(Textos.t("plantilla.noEncontrada", recurso));
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void aplicar(Plantilla p, Marcadores m) {
        p.setListaMarcadores(m.simples());
        p.setListaColumnasTabla(m.columnasTabla());
        p.setTieneTabla(m.tieneTabla());
    }
}
