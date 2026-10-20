package cl.duoc.dsy1107.ms_notificaciones.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;

@Service
public class NotificacionService {
    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final Set<String> procesados = ConcurrentHashMap.newKeySet();

    public synchronized void procesar(JsonNode evento) {
        String eventId = evento.get("eventId").asText();
        if (procesados.contains(eventId)) {
            log.info("Evento duplicado ignorado: eventId={}", eventId);
            return;
        }

        //Solo para demostrar reintentos y DLQ: un título "FALLAR" simula un fallo de procesamiento
        if ("FALLAR".equals(evento.path("titulo").asText())) {
            throw new IllegalStateException("Fallo simulado de procesamiento");
        }

        String tipo = evento.get("tipo").asText();
        switch (tipo) {
            case "prestamo.creado" -> log.info(
                    "NOTIFICACIÓN: préstamo {} creado para usuario={} (libro='{}', devolución estimada={}). eventId={}",
                    evento.get("prestamoId").asLong(), evento.get("usuario").asText(),
                    evento.get("titulo").asText(), evento.get("fechaDevolucionEstimada").asText(), eventId);
            case "prestamo.devuelto" -> log.info(
                    "NOTIFICACIÓN: préstamo {} devuelto por usuario={} (libro='{}'). eventId={}",
                    evento.get("prestamoId").asLong(), evento.get("usuario").asText(),
                    evento.get("titulo").asText(), eventId);
            case "stock.agotado" -> log.warn(
                    "ALERTA STOCK: libro {} ('{}') sin unidades disponibles. eventId={}",
                    evento.get("libroId").asLong(), evento.get("titulo").asText(), eventId);
            default -> throw new IllegalArgumentException("Tipo inesperado: " + tipo);
        }
        procesados.add(eventId);
    }
}
