package cl.duoc.dsy1107.ms_historial.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import cl.duoc.dsy1107.ms_historial.event.PrestamoEventoDto;
import cl.duoc.dsy1107.ms_historial.kafka.EventoJsonDeserializer;

@Configuration
public class KafkaConsumerConfig {

    private static final Logger log =
            LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            @Value("${app.kafka.reintentos}") long reintentos,
            @Value("${app.kafka.espera-ms}") long esperaMs) {

        return new DefaultErrorHandler(
                (registro, error) -> log.error(
                        "Mensaje descartado: tópico={} partición={} offset={} motivo={}",
                        registro.topic(),
                        registro.partition(),
                        registro.offset(),
                        error.getMessage()),
                new FixedBackOff(esperaMs, reintentos));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PrestamoEventoDto>
            prestamoListenerFactory(
                    @Value("${spring.kafka.bootstrap-servers}") String servidores,
                    DefaultErrorHandler kafkaErrorHandler) {

        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servidores);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        ConsumerFactory<String, PrestamoEventoDto> consumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        props,
                        new StringDeserializer(),
                        new ErrorHandlingDeserializer<>(
                                new EventoJsonDeserializer<>(PrestamoEventoDto.class)));

        ConcurrentKafkaListenerContainerFactory<String, PrestamoEventoDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);

        return factory;
    }
}