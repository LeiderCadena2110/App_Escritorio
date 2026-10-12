package co.edu.ucc.reportes.repository;

import co.edu.ucc.reportes.domain.Ejecucion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EjecucionRepositorio extends JpaRepository<Ejecucion, Long> {

    List<Ejecucion> findAllByOrderByFechaDesc();

    List<Ejecucion> findTop5ByOrderByFechaDesc();
}
