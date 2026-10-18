package cl.duoc.dsy1107.ms_rabbit_admin.exception;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Object> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(cuerpo(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidacionBody(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("campo", fe.getField(),
                        "mensaje", String.valueOf(fe.getDefaultMessage())))
                .toList();
        return respuestaValidacion(errores);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Object> handleValidacionParametros(HandlerMethodValidationException ex) {
        List<Map<String, String>> errores = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> Map.of("campo", r.getMethodParameter().getParameterName(),
                                "mensaje", String.valueOf(e.getDefaultMessage()))))
                .toList();
        return respuestaValidacion(errores);
    }

    @ExceptionHandler(AmqpConnectException.class)
    public ResponseEntity<Object> handleRabbitCaido(AmqpConnectException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(cuerpo(HttpStatus.SERVICE_UNAVAILABLE, "No hay conexión con RabbitMQ"));
    }

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<Object> handleConflictoRabbit(AmqpException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(cuerpo(HttpStatus.CONFLICT,
                        "RabbitMQ rechazó la operación (¿ya existe con otros argumentos?)"));
    }

    private ResponseEntity<Object> respuestaValidacion(List<Map<String, String>> errores) {
        Map<String, Object> body = cuerpo(HttpStatus.BAD_REQUEST, "La petición no es válida");
        body.put("errores", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private Map<String, Object> cuerpo(HttpStatus status, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("mensaje", mensaje);
        return body;
    }
}