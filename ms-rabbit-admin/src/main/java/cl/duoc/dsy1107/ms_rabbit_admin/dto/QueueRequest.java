package cl.duoc.dsy1107.ms_rabbit_admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record QueueRequest(

    @NotBlank(message = "el nombre es obligatorio")
    @Pattern (regexp = Reglas.NOMBRE_REGEX, message = Reglas.NOMBRE_MENSAJE)
    String name,

    Boolean durable,

    Boolean quorum,

    @Pattern(regexp = Reglas.NOMBRE_REGEX, message = Reglas.NOMBRE_MENSAJE)
    String deadLetterExchange,

    @Pattern(regexp = Reglas.NOMBRE_REGEX, message = Reglas.NOMBRE_MENSAJE)
    String deadLetterRoutingKey) {

    public QueueRequest {
        
        if (durable == null) {
            durable = true;
        }

        if (quorum == null) {
            quorum = true;
        }
    }

    @JsonIgnore 
    @AssertTrue(message = "si se indica deadLetterRoutingKey también debe indicarse deadLetterExchange")
    public boolean isDeadLetterExchangeRequerido() {
        boolean hayRoutingKey = deadLetterRoutingKey != null && !deadLetterRoutingKey.isBlank();
        boolean hayExchange = deadLetterExchange != null && !deadLetterExchange.isBlank();
        return !hayRoutingKey || hayExchange;
    }

    @JsonIgnore
    @AssertTrue(message = "una cola quorum debe ser durable")
    public boolean isQuorumDurable() {
        return !quorum || durable;
    }

}
