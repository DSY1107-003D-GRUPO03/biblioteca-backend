package cl.duoc.dsy1107.biblioteca_backend.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;



@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(Exchanges exchange, Queues queue, RoutingKeys rk) {
    
    public static final String DLQ_SUFFIX = ".dlq";

    public record Exchanges(String events, String direct, String dlx) {}

    public record Queues(String notificaciones, String historial, String alertasStock) {}

    public record RoutingKeys(String libroCreado, String libroEliminado, String stockAgotado) {}
}
