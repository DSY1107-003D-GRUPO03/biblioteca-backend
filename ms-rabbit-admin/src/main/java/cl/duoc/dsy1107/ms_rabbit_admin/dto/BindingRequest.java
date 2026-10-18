package cl.duoc.dsy1107.ms_rabbit_admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BindingRequest(

    @NotBlank(message = "el exchange es obligatorio")
    @Pattern(regexp = Reglas.NOMBRE_REGEX, message = Reglas.NOMBRE_MENSAJE)
    String exchange,

    @NotBlank(message = "la cola es obligatoria")
    @Pattern(regexp = Reglas.NOMBRE_REGEX, message = Reglas.NOMBRE_MENSAJE)
    String queue,

    @NotBlank(message = "la routing key es obligatoria")
    @Size(max = 255, message = "la routing key no puede superar los 255 caracteres")
    String routingKey) {
}
