package cl.duoc.dsy1107.ms_kafka_admin.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record UpdateTopicConfigRequest(

    @Min(value = -1, message = "retentionMs debe ser mayor o igual a -1")
    Long retentionMs,

    @Pattern(
        regexp = "^(delete|compact|compact,delete|delete,compact)$",
        message = "cleanupPolicy debe ser delete, compact o compact,delete"
    )
    String cleanupPolicy,

    Map<String, String> configs
) {
    @JsonIgnore
    @AssertTrue(message = "debe indicar al menos una configuración para actualizar")
    public boolean isAlMenosUnaConfiguracion() {
        return retentionMs != null
                || (cleanupPolicy != null && !cleanupPolicy.isBlank())
                || (configs != null && !configs.isEmpty());
    }
}