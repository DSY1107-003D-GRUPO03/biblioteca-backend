package cl.duoc.dsy1107.ms_analitica.event;

import java.time.Instant;

public record PrestamoEventoDto(
    String eventId,
    String tipo,
    Long prestamoId,
    Long libroId,
    String titulo,
    String usuario,
    Instant fecha
) {}
