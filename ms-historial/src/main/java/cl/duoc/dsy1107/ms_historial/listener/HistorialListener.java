package cl.duoc.dsy1107.ms_historial.listener;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.rabbitmq.client.Channel;
import cl.duoc.dsy1107.ms_historial.service.HistorialService;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;
import cl.duoc.dsy1107.ms_historial.config.RabbitConfig;

@Component
@RequiredArgsConstructor
public class HistorialListener {

    private static final Logger log =
            LoggerFactory.getLogger(HistorialListener.class);

    private final HistorialService historialService;
    private final ObjectMapper mapper;

    @RabbitListener(queues = RabbitConfig.QUEUE_HISTORIAL)
    public void recibir(Message mensaje, Channel canal) throws IOException {

        long deliveryTag =
                mensaje.getMessageProperties().getDeliveryTag();

        // El JSON se lee aquí (y no en el conversor de Spring) para rechazar
        // de forma explícita los mensajes que no se pueden interpretar.
        Map<String, Object> evento;
        try {
            evento = leerEvento(mensaje.getBody());
        } catch (Exception e) {
            evento = null;
        }

        if (evento == null) {
            log.error(
                    "Evento inválido enviado a DLQ. eventId=desconocido, cola=historial.queue, causa=JSON inválido"
            );

            canal.basicNack(deliveryTag, false, false);
            return;
        }

        String eventId = String.valueOf(evento.get("eventId"));
        long[] esperas = {1000, 2000, 4000};

        if (evento.get("eventId") == null
        || evento.get("tipo") == null
        || evento.get("libroId") == null
        || (evento.get("tipo").toString().startsWith("prestamo.")
            && (evento.get("prestamoId") == null
                || evento.get("usuario") == null))) {

            log.error(
                    "Evento inválido enviado a DLQ. eventId={}, cola=historial.queue, causa=Faltan campos obligatorios",
                    eventId
            );

            canal.basicNack(deliveryTag, false, false);
            return;
        }

        for (int intento = 0; intento <= 3; intento++) {
            try {
                historialService.registrar(evento);
                canal.basicAck(deliveryTag, false);
                return;

            } catch (Exception e) {
                if (intento == 3) {
                    log.error(
                            "Evento enviado a DLQ. eventId={}, cola=historial.queue, causa={}",
                            eventId, e.getMessage(), e
                    );

                    canal.basicNack(deliveryTag, false, false);
                    return;
                }

                log.warn(
                        "Reintento {}/3 para eventId={}. Causa: {}",
                        intento + 1, eventId, e.getMessage()
                );

                try {
                    Thread.sleep(esperas[intento]);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    canal.basicNack(deliveryTag, false, false);
                    return;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> leerEvento(byte[] cuerpo) {
        return mapper.readValue(cuerpo, Map.class);
    }
}