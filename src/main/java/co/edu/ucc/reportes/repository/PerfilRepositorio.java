package co.edu.ucc.reportes.repository;

import co.edu.ucc.reportes.domain.PerfilMapeo;
import co.edu.ucc.reportes.domain.Plantilla;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilRepositorio extends JpaRepository<PerfilMapeo, Long> {

    List<PerfilMapeo> findAllByOrderByNombreAsc();

    List<PerfilMapeo> findByPlantilla(Plantilla plantilla);
}
