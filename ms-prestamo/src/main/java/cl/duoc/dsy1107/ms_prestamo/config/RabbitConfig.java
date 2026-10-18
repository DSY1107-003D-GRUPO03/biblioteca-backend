package cl.duoc.dsy1107.ms_prestamo.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Map;
import org.springframework.amqp.core.QueueBuilder;


@Configuration 
public class RabbitConfig {
    
    @Bean
    public ApplicationRunner abirConexion(ConnectionFactory connectionFactory) {
        return args -> connectionFactory.createConnection().close();
    }

    public static final String EXCHANGE = "biblioteca.events";
    public static final String QUEUE_NOTIFICACIONES = "notificaciones.queue";
    public static final String QUEUE_HISTORIAL = "historial.queue";
    public static final String RK_CREADO = "prestamo.creado";
    public static final String RK_DEVUELTO = "prestamo.devuelto";
    public static final String RK_PATRON = "prestamo.*";
    public static final String EXCHANGE_DLX = "biblioteca.dlx";
    public static final String DLQ_NOTIFICACIONES = "notificaciones.queue.dlq";
    public static final String DLQ_HISTORIAL = "historial.queue.dlq";
    
    @Bean 
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(EXCHANGE_DLX, true, false);
    }
        @Bean
    public Queue notificacionesQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICACIONES)
                .quorum()
                .deadLetterExchange(EXCHANGE_DLX)
                .deadLetterRoutingKey(DLQ_NOTIFICACIONES)
                .build();
    }

    @Bean
    public Queue historialQueue() {
        return QueueBuilder.durable(QUEUE_HISTORIAL)
                .quorum()
                .deadLetterExchange(EXCHANGE_DLX)
                .deadLetterRoutingKey(DLQ_HISTORIAL)
                .build();
    }

    @Bean
    public Queue dlqNotificaciones() {
        return QueueBuilder.durable(DLQ_NOTIFICACIONES)
                .quorum()
                .build();
    }

    @Bean
    public Queue dlqHistorial() {
        return QueueBuilder.durable(DLQ_HISTORIAL)
                .quorum()
                .build();
    }

    @Bean
    public Binding bindingDlqNotificaciones(
            Queue dlqNotificaciones, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(dlqNotificaciones)
                .to(deadLetterExchange)
                .with(DLQ_NOTIFICACIONES);
    }

    @Bean
    public Binding bindingDlqHistorial(
            Queue dlqHistorial, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(dlqHistorial)
                .to(deadLetterExchange)
                .with(DLQ_HISTORIAL);
    }

    @Bean 
    public Binding bindingNotificaciones(Queue notificacionesQueue, TopicExchange exchange){
        return BindingBuilder.bind(notificacionesQueue).to(exchange).with(RK_PATRON);
    }

    @Bean 
    public Binding bindingHistorial(Queue historialQueue, TopicExchange exchange) {
        return BindingBuilder.bind(historialQueue).to(exchange).with(RK_PATRON);
    }

    @Bean 
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter(); //los msg viajan como JSON
    }
}
