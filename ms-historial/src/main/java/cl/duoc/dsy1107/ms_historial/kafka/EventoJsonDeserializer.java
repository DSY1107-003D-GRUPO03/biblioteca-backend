package cl.duoc.dsy1107.ms_historial.kafka;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

public class EventoJsonDeserializer<T> implements Deserializer<T> {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final Class<T> tipo;

    public EventoJsonDeserializer(Class<T> tipo) {
        this.tipo = tipo;
    }

    @Override
    public T deserialize(String topic, byte[] datos) {
        if (datos == null) {
            return null;
        }

        try {
            return MAPPER.readValue(datos, tipo);
        } catch (RuntimeException e) {
            throw new SerializationException(
                    "No se pudo leer el mensaje del tópico " + topic, e);
        }
    }
}