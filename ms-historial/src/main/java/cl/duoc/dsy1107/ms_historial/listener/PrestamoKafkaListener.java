package cl.duoc.dsy1107.ms_historial.listener;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import cl.duoc.dsy1107.ms_historial.event.PrestamoEventoDto;
import cl.duoc.dsy1107.ms_historial.service.HistorialService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PrestamoKafkaListener {

    private static final Logger log =
            LoggerFactory.getLogger(PrestamoKafkaListener.class);

    private final HistorialService historialService;

    @KafkaListener(
            topics = "${app.kafka.topic-prestamos}",
            groupId = "${app.kafka.group-prestamos}",
            containerFactory = "prestamoListenerFactory")
    public void escuchar(ConsumerRecord<String, PrestamoEventoDto> registro) {

        PrestamoEventoDto evento = registro.value();

        if (evento == null) {
            log.warn("Se recibió un evento vacío desde Kafka");
            return;
        }

        log.info("Evento recibido de {} partición {} offset {} clave {}",
                registro.topic(),
                registro.partition(),
                registro.offset(),
                registro.key());

        Map<String, Object> datos = new HashMap<>();

        datos.put("eventId", evento.eventId());
        datos.put("tipo", evento.tipo());
        datos.put("prestamoId", evento.prestamoId());
        datos.put("libroId", evento.libroId());
        datos.put("usuario", evento.usuario());

        if (evento.fecha() != null) {
            datos.put("ocurridoEn", evento.fecha().toString());
        }

        historialService.registrar(datos);
    }
}