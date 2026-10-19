
package cl.duoc.dsy1107.ms_notificaciones.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {

    // 1. EXCHANGES

    @Bean
    public TopicExchange eventsExchange(
            @Value("${app.rabbit.exchanges.events}") String nombre) {
        return new TopicExchange(nombre, true, false);
    }

    @Bean
    public DirectExchange directExchange(
            @Value("${app.rabbit.exchanges.direct}") String nombre) {
        return new DirectExchange(nombre, true, false);
    }

    @Bean
    public DirectExchange dlxExchange(
            @Value("${app.rabbit.exchanges.dlx}") String nombre) {
        return new DirectExchange(nombre, true, false);
    }

    // 2. COLAS PRINCIPALES

    @Bean
    public Queue notificacionesQueue(
            @Value("${app.rabbit.queues.notificaciones}") String nombre,
            @Value("${app.rabbit.exchanges.dlx}") String dlx,
            @Value("${app.rabbit.queues.notificaciones-dlq}") String dlq) {

        return QueueBuilder.durable(nombre)
                .quorum()
                .deadLetterExchange(dlx)
                .deadLetterRoutingKey(dlq)
                .build();
    }

    @Bean
    public Queue stockQueue(
            @Value("${app.rabbit.queues.stock}") String nombre,
            @Value("${app.rabbit.exchanges.dlx}") String dlx,
            @Value("${app.rabbit.queues.stock-dlq}") String dlq) {

        return QueueBuilder.durable(nombre)
                .quorum()
                .deadLetterExchange(dlx)
                .deadLetterRoutingKey(dlq)
                .build();
    }

    // 3. COLAS DE MENSAJES FALLIDOS (DLQ)

    @Bean
    public Queue notificacionesDlq(
            @Value("${app.rabbit.queues.notificaciones-dlq}") String nombre) {
        return QueueBuilder.durable(nombre).quorum().build();
    }

    @Bean
    public Queue stockDlq(
            @Value("${app.rabbit.queues.stock-dlq}") String nombre) {
        return QueueBuilder.durable(nombre).quorum().build();
    }

    // 4. BINDINGS PRINCIPALES

    @Bean
    public Binding notificacionesBinding(
            @Qualifier("notificacionesQueue") Queue cola,
            @Qualifier("eventsExchange") TopicExchange exchange,
            @Value("${app.rabbit.routes.prestamos}") String ruta) {

        return BindingBuilder.bind(cola).to(exchange).with(ruta);
    }

    @Bean
    public Binding stockBinding(
            @Qualifier("stockQueue") Queue cola,
            @Qualifier("directExchange") DirectExchange exchange,
            @Value("${app.rabbit.routes.stock}") String ruta) {

        return BindingBuilder.bind(cola).to(exchange).with(ruta);
    }

    // 5. BINDINGS DE LAS DLQ

    @Bean
    public Binding notificacionesDlqBinding(
            @Qualifier("notificacionesDlq") Queue cola,
            @Qualifier("dlxExchange") DirectExchange exchange,
            @Value("${app.rabbit.queues.notificaciones-dlq}") String ruta) {

        return BindingBuilder.bind(cola).to(exchange).with(ruta);
    }

    @Bean
    public Binding stockDlqBinding(
            @Qualifier("stockDlq") Queue cola,
            @Qualifier("dlxExchange") DirectExchange exchange,
            @Value("${app.rabbit.queues.stock-dlq}") String ruta) {

        return BindingBuilder.bind(cola).to(exchange).with(ruta);
    }
}
