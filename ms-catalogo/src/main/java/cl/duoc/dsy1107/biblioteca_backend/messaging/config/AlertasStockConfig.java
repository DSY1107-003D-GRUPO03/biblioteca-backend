package cl.duoc.dsy1107.biblioteca_backend.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration 
public class AlertasStockConfig {
    
    @Bean 
    DirectExchange directExchange(MessagingProperties p) {
        return new DirectExchange(p.exchange().direct(), true, false);
    }

    @Bean 
    Queue alertasStockQueue(MessagingProperties p) {
        return QueueBuilder.durable(p.queue().alertasStock())
            .quorum()
            .deadLetterExchange(p.exchange().dlx())
            .deadLetterRoutingKey(p.queue().alertasStock() + MessagingProperties.DLQ_SUFFIX)
            .build();
    }

    @Bean 
    Queue alertasStockDlq(MessagingProperties p) {
        return QueueBuilder.durable(p.queue().alertasStock() + MessagingProperties.DLQ_SUFFIX)
            .quorum()
            .build();
    }

    @Bean 
    Binding alertasStockBinding(Queue alertasStockQueue, DirectExchange directExchange, MessagingProperties p) {
        return BindingBuilder.bind(alertasStockQueue).to(directExchange).with(p.rk().stockAgotado());
    }

    @Bean 
    Binding alertasStockDlqBinding(Queue alertasStockDlq, DirectExchange dlxExchange, MessagingProperties p) {
        return BindingBuilder.bind(alertasStockDlq).to(dlxExchange)
            .with(p.queue().alertasStock() + MessagingProperties.DLQ_SUFFIX);
    }

}
