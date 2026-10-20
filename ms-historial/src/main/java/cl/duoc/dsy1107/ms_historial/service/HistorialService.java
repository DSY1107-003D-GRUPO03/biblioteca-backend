package cl.duoc.dsy1107.ms_historial.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cl.duoc.dsy1107.ms_historial.dto.EntradaHistorial;
import cl.duoc.dsy1107.ms_historial.model.HistorialEntry;
import cl.duoc.dsy1107.ms_historial.repository.HistorialRepository;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class HistorialService {
    
    private static final Logger log = LoggerFactory.getLogger(HistorialService.class);
    private final HistorialRepository historialRepository;

    public List<EntradaHistorial> listar(String tipo, String usuario) {
        tipo = limpiar(tipo);
        usuario = limpiar(usuario);

        List<HistorialEntry> registros;
        if (tipo != null && usuario != null) {
            registros = historialRepository.findByTipoEventoAndUsuarioOrderByRecibidoEnDescIdDesc(tipo, usuario);
        } else if (tipo != null) {
            registros = historialRepository.findByTipoEventoOrderByRecibidoEnDescIdDesc(tipo);
        } else if (usuario != null) {
            registros = historialRepository.findByUsuarioOrderByRecibidoEnDescIdDesc(usuario);
        } else {
            registros = historialRepository.findAllByOrderByRecibidoEnDescIdDesc();
        }
        return registros.stream().map(EntradaHistorial::desde).toList();
    }

    private String limpiar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    public void registrar(Map<String, Object> evento) {
        String eventId = (String) evento.get("eventId");
        if (eventId != null && historialRepository.existsByEventId(eventId)){
            log.warn("Evento duplicado ignorado: {}", eventId);
            return;
        }

        HistorialEntry entry = new HistorialEntry();
        entry.setEventId(eventId);
        entry.setTipoEvento((String) evento.get("tipo"));
        entry.setPrestamoId(aLong(evento.get("prestamoId")));
        entry.setLibroId(aLong(evento.get("libroId")));
        entry.setUsuario((String) evento.get("usuario"));


        // Obtener la fecha segun el tipo de evento
        Object fecha = evento.get("fechaPrestamo");

        if (fecha == null) {
            fecha = evento.get("fechaDevolucionReal");
        }

        if (fecha == null) {
            fecha = evento.get("ocurridoEn");
        }

        // Guardar la fecha original del evento
        entry.setFecha(
                fecha != null
                        ? Instant.parse(fecha.toString())
                        : Instant.now()
        );

        // Registrar cuando llego el evento al historial
        entry.setRecibidoEn(Instant.now());

        // Guardar el registro en MySQL
        historialRepository.save(entry);

        log.info("Historial registrado: {} | préstamo {} | libro {} | usuario {}",
            entry.getTipoEvento(), entry.getPrestamoId(), entry.getLibroId(), entry.getUsuario());
    }

    private Long aLong(Object valor) {
        return valor == null ? null : ((Number) valor).longValue();
    }

}
