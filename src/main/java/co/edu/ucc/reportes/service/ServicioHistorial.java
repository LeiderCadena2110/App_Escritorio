package co.edu.ucc.reportes.service;

import co.edu.ucc.reportes.domain.Ejecucion;
import co.edu.ucc.reportes.domain.EstadoEjecucion;
import co.edu.ucc.reportes.repository.EjecucionRepositorio;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

/** Historial de reportes generados. */
@Service
public class ServicioHistorial {

    private final EjecucionRepositorio repo;

    public ServicioHistorial(EjecucionRepositorio repo) {
        this.repo = repo;
    }

    public List<Ejecucion> listar() {
        return repo.findAllByOrderByFechaDesc();
    }

    public List<Ejecucion> recientes() {
        return repo.findTop5ByOrderByFechaDesc();
    }

    public Ejecucion registrar(GeneradorInforme.Solicitud s, GeneradorInforme.Resultado r, long duracionMs) {
        Ejecucion e = new Ejecucion();
        e.setFecha(LocalDateTime.now());
        e.setPlantillaNombre(s.plantilla().getNombre());
        e.setArchivoDatos(s.tabla().archivo() == null ? "" : s.tabla().archivo().toString());
        e.setModo(s.modo());
        e.setFormato(s.plantilla().getTipo().extensionSalida());
        e.setCarpetaSalida(r.carpeta() == null ? s.carpetaBase().toString() : r.carpeta().toString());
        e.setFilasProcesadas(r.filasProcesadas());
        e.setInformesGenerados(r.generados());
        e.setEstado(r.cancelado() ? EstadoEjecucion.CANCELADA
                : r.errores().isEmpty() ? EstadoEjecucion.COMPLETADA
                : r.generados() == 0 ? EstadoEjecucion.FALLIDA
                : EstadoEjecucion.CON_ERRORES);
        String mensajes = String.join("\n", r.errores());
        e.setMensajes(mensajes.length() > 7900 ? mensajes.substring(0, 7900) + "\n…" : mensajes);
        e.setDuracionMs(duracionMs);
        return repo.save(e);
    }

    public void borrarTodo() {
        repo.deleteAll();
    }
}
