package cl.duoc.dsy1107.ms_notificaciones.messaging.listener;

import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import cl.duoc.dsy1107.ms_notificaciones.messaging.processor.MessageProcessor;

@Component
public class StockListener {

    private final MessageProcessor processor;
    private final String colaStock;

    public StockListener(
            MessageProcessor processor,
            @Value("${app.rabbit.queues.stock}") String cola) {

        this.processor = processor;
        this.colaStock = cola;
    }

    @RabbitListener(queues = "${app.rabbit.queues.stock}")
    public void recibirStock(Message mensaje, Channel canal)
            throws IOException {

        processor.procesar(mensaje, canal, colaStock, true);
    }
}
