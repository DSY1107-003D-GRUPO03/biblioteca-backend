package cl.duoc.dsy1107.ms_analitica.event;

import java.time.Instant;

public record CatalogoEventoDto(
    String eventId,
    String tipo,
    Long libroId,
    String titulo,
    Integer stock,
    Instant fecha
) {}
