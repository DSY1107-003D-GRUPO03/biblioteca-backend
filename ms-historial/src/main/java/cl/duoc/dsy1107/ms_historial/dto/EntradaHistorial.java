package cl.duoc.dsy1107.ms_historial.dto;

import java.time.Instant;
import cl.duoc.dsy1107.ms_historial.model.HistorialEntry;

// Forma de respuesta indicada en docs/contratos/api-rest.md
public record EntradaHistorial(
        Long id,
        String eventId,
        String tipo,
        String usuario,
        Long libroId,
        Long prestamoId,
        Instant recibidoEn
) {
    public static EntradaHistorial desde(HistorialEntry registro) {
        return new EntradaHistorial(
                registro.getId(), registro.getEventId(), registro.getTipoEvento(),
                registro.getUsuario(), registro.getLibroId(), registro.getPrestamoId(),
                registro.getRecibidoEn());
    }
}
