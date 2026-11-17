package cl.duoc.dsy1107.ms_prestamo.kafka;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import cl.duoc.dsy1107.ms_prestamo.event.PrestamoEventoDto;
import cl.duoc.dsy1107.ms_prestamo.model.Prestamo;

@Component 
public class PrestamoKafkaPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(PrestamoKafkaPublisher.class);

    private final KafkaTemplate<String, PrestamoEventoDto> kafkaTemplate;
    private final String topic;

    public PrestamoKafkaPublisher(
            KafkaTemplate<String, PrestamoEventoDto> kafkaTemplate,
            @Value("${app.kafka.topic-prestamos}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publicarCreado(Prestamo p) {
        publicar(new PrestamoEventoDto(UUID.randomUUID().toString(), "CREADO",
                p.getId(), p.getLibroId(), p.getTitulo(), p.getUsuario(), p.getFechaPrestamo()));
    }

    public void publicarDevuelto(Prestamo p) {
        publicar(new PrestamoEventoDto(UUID.randomUUID().toString(), "DEVUELTO",
                p.getId(), p.getLibroId(), p.getTitulo(), p.getUsuario(), p.getFechaDevolucionReal()));
    }

    private void publicar(PrestamoEventoDto evento) {
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
