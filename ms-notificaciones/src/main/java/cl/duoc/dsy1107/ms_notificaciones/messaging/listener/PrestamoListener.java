package cl.duoc.dsy1107.ms_notificaciones.messaging.listener;

import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import cl.duoc.dsy1107.ms_notificaciones.messaging.processor.MessageProcessor;

@Component
public class PrestamoListener {

    private final MessageProcessor processor;
    private final String colaNotificaciones;

    public PrestamoListener(
            MessageProcessor processor,
            @Value("${app.rabbit.queues.notificaciones}") String cola) {

        this.processor = processor;
        this.colaNotificaciones = cola;
    }

    @RabbitListener(queues = "${app.rabbit.queues.notificaciones}")
    public void recibirPrestamo(Message mensaje, Channel canal)
            throws IOException {

        processor.procesar(mensaje, canal, colaNotificaciones, false);
    }
}
