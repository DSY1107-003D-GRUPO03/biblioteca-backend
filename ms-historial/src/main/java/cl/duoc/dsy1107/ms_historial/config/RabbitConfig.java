package cl.duoc.dsy1107.ms_historial.config;

import java.util.Map;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.util.ErrorHandler;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "biblioteca.events";
    public static final String QUEUE_HISTORIAL = "historial.queue";
    public static final String EXCHANGE_DLX = "biblioteca.dlx";
    public static final String DLQ_HISTORIAL = "historial.queue.dlq";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public org.springframework.amqp.core.DirectExchange deadLetterExchange() {
        return new org.springframework.amqp.core.DirectExchange(
                EXCHANGE_DLX, true, false);
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
    public Queue dlqHistorial() {
        return QueueBuilder.durable(DLQ_HISTORIAL)
                .quorum()
                .build();
    }

    @Bean
    public Binding bindingHistorial(
            Queue historialQueue, TopicExchange exchange) {
        return BindingBuilder.bind(historialQueue)
                .to(exchange)
                .with("prestamo.*");
    }

    @Bean
    public Binding bindingDlqHistorial(
            Queue dlqHistorial,
            org.springframework.amqp.core.DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(dlqHistorial)
                .to(deadLetterExchange)
                .with(DLQ_HISTORIAL);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        factory.setErrorHandler(new ConditionalRejectingErrorHandler());

        return factory;
    }
}
