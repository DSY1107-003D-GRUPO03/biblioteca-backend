package cl.duoc.dsy1107.ms_historial.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import cl.duoc.dsy1107.ms_historial.model.HistorialEntry;

public interface HistorialRepository extends JpaRepository<HistorialEntry, Long> {

    boolean existsByEventId(String eventId);

    List<HistorialEntry> findAllByOrderByRecibidoEnDescIdDesc();

    List<HistorialEntry> findByTipoEventoOrderByRecibidoEnDescIdDesc(String tipoEvento);

    List<HistorialEntry> findByUsuarioOrderByRecibidoEnDescIdDesc(String usuario);

    List<HistorialEntry> findByTipoEventoAndUsuarioOrderByRecibidoEnDescIdDesc(
            String tipoEvento, String usuario);
}
