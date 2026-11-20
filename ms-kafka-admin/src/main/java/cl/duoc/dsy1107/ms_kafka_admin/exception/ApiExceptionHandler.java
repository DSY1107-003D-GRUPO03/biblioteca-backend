package cl.duoc.dsy1107.ms_kafka_admin.exception;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private record ErrorCampo(String campo, String mensaje) {}

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Object> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(cuerpo(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(RecursoYaExisteException.class)
    public ResponseEntity<Object> handleYaExiste(RecursoYaExisteException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(cuerpo(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleArgumentoInvalido(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(cuerpo(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidacionBody(MethodArgumentNotValidException ex) {
        List<ErrorCampo> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorCampo(fe.getField(), String.valueOf(fe.getDefaultMessage())))
                .toList();
        return respuestaValidacion(errores);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Object> handleValidacionParametros(HandlerMethodValidationException ex) {
        List<ErrorCampo> errores = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new ErrorCampo(
                                String.valueOf(r.getMethodParameter().getParameterName()),
                                String.valueOf(e.getDefaultMessage()))))
                .toList();
        return respuestaValidacion(errores);
    }

    @ExceptionHandler(KafkaNoDisponibleException.class)
    public ResponseEntity<Object> handleKafkaCaido(KafkaNoDisponibleException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(cuerpo(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage()));
    }

    private ResponseEntity<Object> respuestaValidacion(List<ErrorCampo> errores) {
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