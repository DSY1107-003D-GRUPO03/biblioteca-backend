package cl.duoc.dsy1107.ms_rabbit_admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ExchangeRequest(
    
    @NotBlank(message = "el nombre es obligatorio")
    @Pattern(regexp = Reglas.NOMBRE_REGEX, message = Reglas.NOMBRE_MENSAJE)
    String name,

    @NotBlank(message = "el tipo es obligatorio")
    @Pattern(regexp = "^(direct|topic|fanout)$", message = "el tipo debe ser direct, topic o fanout")
    String type,

    Boolean durable ) {

    public ExchangeRequest {
        if (durable == null) {
            durable = true;
        }
    }
}
