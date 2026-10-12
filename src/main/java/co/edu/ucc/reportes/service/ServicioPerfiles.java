package co.edu.ucc.reportes.service;

import co.edu.ucc.reportes.domain.ModoSalida;
import co.edu.ucc.reportes.domain.PerfilMapeo;
import co.edu.ucc.reportes.domain.Plantilla;
import co.edu.ucc.reportes.repository.PerfilRepositorio;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Perfiles de mapeo: el mismo reporte se repite con un archivo de datos nuevo. */
@Service
public class ServicioPerfiles {

    private final PerfilRepositorio repo;

    public ServicioPerfiles(PerfilRepositorio repo) {
        this.repo = repo;
    }

    public List<PerfilMapeo> listar() {
        return repo.findAllByOrderByNombreAsc();
    }

    /** Guarda o reemplaza (si ya hay uno con el mismo nombre). */
    public PerfilMapeo guardar(String nombre, Plantilla plantilla, Map<String, String> mapeo, ModoSalida modo, String columnaNombre) {
        PerfilMapeo p = repo.findAll().stream()
                .filter(x -> x.getNombre().equalsIgnoreCase(nombre.trim()))
                .findFirst()
                .orElseGet(PerfilMapeo::new);
        if (p.getCreado() == null) p.setCreado(LocalDateTime.now());
        p.setNombre(nombre.trim());
        p.setPlantilla(plantilla);
        p.setMapeo(mapeo);
        p.setModo(modo);
        p.setColumnaNombre(columnaNombre);
        p.setUltimoUso(LocalDateTime.now());
        return repo.save(p);
    }

    public void marcarUso(PerfilMapeo p) {
        p.setUltimoUso(LocalDateTime.now());
        repo.save(p);
    }

    public void eliminar(PerfilMapeo p) {
        repo.delete(p);
    }
}
