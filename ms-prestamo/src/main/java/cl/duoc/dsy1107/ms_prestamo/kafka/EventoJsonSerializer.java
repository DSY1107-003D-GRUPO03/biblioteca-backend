package cl.duoc.dsy1107.ms_prestamo.kafka;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import tools.jackson.databind.json.JsonMapper;

public class EventoJsonSerializer<T> implements Serializer<T>  {
    
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Override
    public byte[] serialize(String topic, T evento) {
        if (evento == null) {
            return null;
        }

        try {
            return MAPPER.writeValueAsBytes(evento);
        } catch (RuntimeException e) {
            throw new SerializationException("No se pudo serializar el evento para el tópico " + topic, e);
        }

    }

}
