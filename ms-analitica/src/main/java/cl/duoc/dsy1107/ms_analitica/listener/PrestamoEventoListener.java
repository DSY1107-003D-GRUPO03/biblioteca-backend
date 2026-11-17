package cl.duoc.dsy1107.ms_analitica.listener;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import cl.duoc.dsy1107.ms_analitica.event.PrestamoEventoDto;
import cl.duoc.dsy1107.ms_analitica.service.EstadisticasService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PrestamoEventoListener {

    private static final Logger log = LoggerFactory.getLogger(PrestamoEventoListener.class);

    private final EstadisticasService estadisticasService;

    @KafkaListener(
            topics = "${app.kafka.topic-prestamos}",
            groupId = "${app.kafka.group-prestamos}",
            containerFactory = "prestamoListenerFactory")
    public void escuchar(ConsumerRecord<String, PrestamoEventoDto> registro) {
        log.info("Recibido de {} partición {} offset {} clave {}",
                registro.topic(), registro.partition(), registro.offset(), registro.key());
        estadisticasService.registrarPrestamo(registro.value());
    }
}