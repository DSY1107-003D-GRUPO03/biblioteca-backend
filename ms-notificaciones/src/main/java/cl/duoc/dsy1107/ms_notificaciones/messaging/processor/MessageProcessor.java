package cl.duoc.dsy1107.ms_notificaciones.messaging.processor;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import cl.duoc.dsy1107.ms_notificaciones.messaging.event.EventoInvalidoException;
import cl.duoc.dsy1107.ms_notificaciones.messaging.event.EventoValidator;
import cl.duoc.dsy1107.ms_notificaciones.service.NotificacionService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class MessageProcessor {

    private static final Logger log =
            LoggerFactory.getLogger(MessageProcessor.class);

    private static final long[] ESPERAS_MS = {1000, 2000, 4000};

    private final EventoValidator validator;
    private final NotificacionService service;
    private final ObjectMapper mapper;

    public MessageProcessor(EventoValidator validator,
                            NotificacionService service,
                            ObjectMapper mapper) {
        this.validator = validator;
        this.service = service;
        this.mapper = mapper;
    }

    public void procesar(Message mensaje, Channel canal,
                         String cola, boolean esStock) throws IOException {

        long tag = mensaje.getMessageProperties().getDeliveryTag();
        String eventId = obtenerEventId(mensaje);

        JsonNode evento;

        try {
            evento = validator.leerYValidar(mensaje.getBody(), esStock);
        } catch (EventoInvalidoException e) {
            log.error("Mensaje inválido. eventId={}, cola={}, causa={}",
                    eventId, cola, e.getMessage());

            canal.basicNack(tag, false, false);
            return;
        }

        for (int intento = 0; intento <= ESPERAS_MS.length; intento++) {
            try {
                service.procesar(evento);
            } catch (Exception e) {

                if (intento == ESPERAS_MS.length) {
                    log.error("Evento rechazado. eventId={}, cola={}, causa={}",
                            eventId, cola, e.getMessage(), e);

                    canal.basicNack(tag, false, false);
                    return;
                }

                log.warn("Reintento {}/3. eventId={}, cola={}, causa={}",
                        intento + 1, eventId, cola, e.getMessage());

                try {
                    Thread.sleep(ESPERAS_MS[intento]);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    canal.basicNack(tag, false, false);
                    return;
                }

                continue;
            }

            canal.basicAck(tag, false);
            return;
        }
    }

    public String obtenerEventId(Message mensaje) {
        try {
            JsonNode evento = mapper.readTree(mensaje.getBody());

            if (evento != null && evento.isObject()
                    && evento.get("eventId") != null) {
                return evento.get("eventId").asText();
            }
        } catch (Exception e) {}

        String id = mensaje.getMessageProperties().getMessageId();
        return id == null ? "desconocido" : id;
    }
}
