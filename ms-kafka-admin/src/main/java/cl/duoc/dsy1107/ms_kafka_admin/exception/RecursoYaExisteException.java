package cl.duoc.dsy1107.ms_kafka_admin.exception;

public class RecursoYaExisteException extends RuntimeException {

    public RecursoYaExisteException(String mensaje) {
        super(mensaje);
    }
}