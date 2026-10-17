package cl.duoc.dsy1107.biblioteca_backend.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration 
public class HistorialLibroConfig {
    
    @Bean 
    TopicExchange eventsExchange(MessagingProperties p) {
        return new TopicExchange(p.exchange().events(), true, false);
    }

    @Bean 
    Queue historialQueue(MessagingProperties p) {
        return QueueBuilder.durable(p.queue().historial())
            .quorum()
            .deadLetterExchange(p.exchange().dlx())
            .deadLetterRoutingKey(p.queue().historial() + MessagingProperties.DLQ_SUFFIX)
            .build();
    }

    @Bean
    Queue historialDlq(MessagingProperties p) {
        return QueueBuilder.durable(p.queue().historial() + MessagingProperties.DLQ_SUFFIX)
            .quorum()
            .build();
    }

    @Bean 
    Binding historialLibroCreado(Queue historialQueue, TopicExchange eventsExchange, MessagingProperties p) {
        return BindingBuilder.bind(historialQueue).to(eventsExchange).with(p.rk().libroCreado());
    }

    @Bean 
    Binding historialLibroEliminado(Queue historialQueue, TopicExchange eventsExchange, MessagingProperties p) {
        return BindingBuilder.bind(historialQueue).to(eventsExchange).with(p.rk().libroEliminado());
    }

    @Bean 
    Binding historialDlqBinding(Queue historialDlq, DirectExchange dlxExchange, MessagingProperties p) {
        return BindingBuilder.bind(historialDlq).to(dlxExchange)
            .with(p.queue().historial() + MessagingProperties.DLQ_SUFFIX);
    }

}
