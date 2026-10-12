package co.edu.ucc.reportes.repository;

import co.edu.ucc.reportes.domain.Plantilla;
import co.edu.ucc.reportes.domain.TipoPlantilla;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantillaRepositorio extends JpaRepository<Plantilla, Long> {

    List<Plantilla> findAllByOrderByIntegradaAscNombreAsc();

    Optional<Plantilla> findFirstByTipoAndRuta(TipoPlantilla tipo, String ruta);
}
