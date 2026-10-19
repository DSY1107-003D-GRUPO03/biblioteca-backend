package cl.duoc.dsy1107.ms_notificaciones.messaging.event;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class EventoValidator {
    private final ObjectMapper mapper;

    public EventoValidator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public JsonNode leerYValidar(byte[] json, boolean colaStock) {
        final JsonNode evento;
        try {
            evento = mapper.readTree(json);
        } catch (Exception e) {
            throw new EventoInvalidoException("JSON inválido", e);
        }

        if (evento == null || !evento.isObject()) {
            throw new EventoInvalidoException("El evento debe ser un objeto JSON");
        }

        String id = requerido(evento, "eventId");
        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new EventoInvalidoException("eventId no es UUID válido: " + id);
        }

        String tipo = requerido(evento, "tipo");
        if (colaStock) {
            if (!"stock.agotado".equals(tipo)) {
                throw new EventoInvalidoException("Evento inesperado en alertas de stock: " + tipo);
            }
            numero(evento, "libroId");
            requerido(evento, "titulo");
            fecha(evento, "ocurridoEn");
        } else {
            if (!"prestamo.creado".equals(tipo) && !"prestamo.devuelto".equals(tipo)) {
                throw new EventoInvalidoException("Evento inesperado en notificaciones: " + tipo);
            }
            numero(evento, "prestamoId");
            numero(evento, "libroId");
            requerido(evento, "titulo");
            requerido(evento, "usuario");
            requerido(evento, "estado");
            if ("prestamo.creado".equals(tipo)) {
                fecha(evento, "fechaPrestamo");
                fecha(evento, "fechaDevolucionEstimada");
            } else {
                fecha(evento, "fechaDevolucionReal");
            }
        }
        return evento;
    }

    private String requerido(JsonNode evento, String campo) {
        JsonNode nodo = evento.get(campo);
        if (nodo == null || !nodo.isTextual() || nodo.asText().isBlank()) {
            throw new EventoInvalidoException("Campo obligatorio inválido: " + campo);
        }
        return nodo.asText();
    }

    private void numero(JsonNode evento, String campo) {
        JsonNode nodo = evento.get(campo);
        if (nodo == null || !nodo.isIntegralNumber() || nodo.asLong() <= 0) {
            throw new EventoInvalidoException("Campo numérico inválido: " + campo);
        }
    }

    private void fecha(JsonNode evento, String campo) {
        try {
            Instant.parse(requerido(evento, campo));
        } catch (DateTimeParseException e) {
            throw new EventoInvalidoException("Fecha ISO-8601 UTC inválida: " + campo);
        }
    }
}
