package cl.duoc.dsy1107.biblioteca_backend.messaging.event;

import java.time.Instant;

public record CatalogoEventoDto (
    String eventId,
    String tipo,
    Long libroId,
    String titulo,
    Integer stock,
    Instant fecha
) {}
