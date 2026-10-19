package cl.duoc.dsy1107.ms_prestamo.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange exchange(@Value("${app.rabbit.exchange}") String nombre) {
        return new TopicExchange(nombre, true, false);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter(); // los mensajes viajan como JSON
    }

    // Abre la conexión al arrancar para que RabbitAdmin declare el exchange de inmediato
    @Bean
    public ApplicationRunner abrirConexion(ConnectionFactory connectionFactory) {
        return args -> connectionFactory.createConnection().close();
    }
}