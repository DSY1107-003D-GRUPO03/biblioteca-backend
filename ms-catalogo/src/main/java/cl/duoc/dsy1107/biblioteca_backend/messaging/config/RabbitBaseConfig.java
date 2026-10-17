package cl.duoc.dsy1107.biblioteca_backend.messaging.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration 
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitBaseConfig {
    
    private static final Logger log = LoggerFactory.getLogger(RabbitBaseConfig.class);

    @Bean
    DirectExchange dlxExchange(MessagingProperties p){
        return new DirectExchange(p.exchange().dlx(), true, false);
    }

    @Bean 
    MessageConverter jsonMessageConverter () {
        return new JacksonJsonMessageConverter();
    }

    //Abre una conexion al arrancar
    @Bean 
    ApplicationRunner declararTopologia(ConnectionFactory connectionFactory) {
        return args -> {
            try {
                connectionFactory.createConnection().close();
            } catch (Exception e) {
                log.warn("RabbitMQ no disponible al arrancar: {}", e.getMessage());
            }
        };
    }


}
