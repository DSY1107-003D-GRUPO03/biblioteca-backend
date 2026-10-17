package cl.duoc.dsy1107.biblioteca_backend.messaging.publisher;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import cl.duoc.dsy1107.biblioteca_backend.messaging.config.MessagingProperties;
import cl.duoc.dsy1107.biblioteca_backend.messaging.event.LibroEvento;
import cl.duoc.dsy1107.biblioteca_backend.model.Libro;
import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class LibroEventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties props;

    public void libroCreado(Libro l) {
        publicar(props.exchange().events(), props.rk().libroCreado(), l);
    }

    public void libroEliminado(Libro l) {
        publicar(props.exchange().events(), props.rk().libroEliminado(), l);
    }

    public void stockAgotado(Libro l) {
        publicar(props.exchange().direct(), props.rk().stockAgotado(), l);
    }

    private void publicar(String exchange, String routingKey, Libro l) {
        LibroEvento evento = LibroEvento.de(routingKey, l.getId(), l.getTitulo(), l.getIsbn(), l.getStock());
        rabbitTemplate.convertAndSend(exchange, routingKey, evento, m -> {
            m.getMessageProperties().setMessageId(evento.eventId());
            m.getMessageProperties().setType(routingKey);
            return m;
        });
    }


}
