package cl.duoc.dsy1107.biblioteca_backend.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record LibroEvento(
    String eventId,
    String tipo,
    Long libroId,
    String titulo,
    String isbn,
    Integer stock,
    Instant ocurriedoEn
) {
    public static LibroEvento de(String tipo, Long libroId, String titulo, String isbn, Integer stock) {
        return new LibroEvento(UUID.randomUUID().toString(), tipo, libroId, titulo, isbn, stock, Instant.now());
    }
}
