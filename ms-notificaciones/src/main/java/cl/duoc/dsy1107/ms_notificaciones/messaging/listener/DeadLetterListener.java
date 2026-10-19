package cl.duoc.dsy1107.ms_notificaciones.messaging.listener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import cl.duoc.dsy1107.ms_notificaciones.messaging.processor.MessageProcessor;

@Component
public class DeadLetterListener {

    private static final Logger log =
            LoggerFactory.getLogger(DeadLetterListener.class);

    private final MessageProcessor processor;
    private final String colaNotificaciones;
    private final String colaStock;

    public DeadLetterListener(
            MessageProcessor processor,
            @Value("${app.rabbit.queues.notificaciones}") String colaNotificaciones,
            @Value("${app.rabbit.queues.stock}") String colaStock) {

        this.processor = processor;
        this.colaNotificaciones = colaNotificaciones;
        this.colaStock = colaStock;
    }

    @RabbitListener(queues = "${app.rabbit.queues.notificaciones-dlq}")
    public void recibirDlqNotificaciones(Message mensaje, Channel canal)
            throws IOException {

        registrarMensaje(mensaje, canal, colaNotificaciones);
    }

    @RabbitListener(queues = "${app.rabbit.queues.stock-dlq}")
    public void recibirDlqStock(Message mensaje, Channel canal)
            throws IOException {

        registrarMensaje(mensaje, canal, colaStock);
    }

    private void registrarMensaje(Message mensaje, Channel canal,
                                  String colaOrigen) throws IOException {

        String eventId = processor.obtenerEventId(mensaje);

        String causa = String.valueOf(
                mensaje.getMessageProperties()
                        .getHeaders()
                        .get("x-first-death-reason")
        );

        String cuerpo = new String(
                mensaje.getBody(), StandardCharsets.UTF_8
        );

        log.error("DLQ: eventId={}, colaOrigen={}, causa={}, cuerpo={}",
                eventId, colaOrigen, causa, cuerpo);

        canal.basicAck(
                mensaje.getMessageProperties().getDeliveryTag(), false
        );
    }
}
