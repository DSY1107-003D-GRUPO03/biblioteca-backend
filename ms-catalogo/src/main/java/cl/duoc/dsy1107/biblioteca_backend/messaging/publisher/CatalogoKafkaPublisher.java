package cl.duoc.dsy1107.biblioteca_backend.messaging.publisher;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import cl.duoc.dsy1107.biblioteca_backend.messaging.event.CatalogoEventoDto;
import cl.duoc.dsy1107.biblioteca_backend.model.Libro;

@Component 
public class CatalogoKafkaPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(CatalogoKafkaPublisher.class);

    private final KafkaTemplate<String, CatalogoEventoDto> kafkaTemplate;
    private final String topic;

    public CatalogoKafkaPublisher(
            KafkaTemplate<String, CatalogoEventoDto> kafkaTemplate,
            @Value ("${app.kafka.topic-catalogo}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void libroCreado(Libro l)       { publicar("CREADO", l); }
    public void libroActualizado(Libro l)  { publicar("ACTUALIZADO", l); }
    public void libroEliminado(Libro l)    { publicar("ELIMINADO", l); }
    public void stockAgotado(Libro l)      { publicar("STOCK_AGOTADO", l); }

    private void publicar(String tipo, Libro l) {
        CatalogoEventoDto evento = new CatalogoEventoDto(
                UUID.randomUUID().toString(), tipo, l.getId(), l.getTitulo(), l.getStock(), Instant.now());
        String clave = String.valueOf(evento.libroId());
        try {
            kafkaTemplate.send(topic, clave, evento).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.error("No se pudo publicar el evento {} ({}) en {}: {}",
                            evento.eventId(), evento.tipo(), topic, error.getMessage());
                } else {
                    var meta = resultado.getRecordMetadata();
                    log.info("Evento {} ({}) publicado en {} partición {} offset {}",
                            evento.eventId(), evento.tipo(), meta.topic(), meta.partition(), meta.offset());
                }
            });
        } catch (RuntimeException e) {
            log.error("Error al enviar el evento {} a Kafka: {}", evento.eventId(), e.getMessage());
        }
    }

}
