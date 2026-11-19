package cl.duoc.dsy1107.ms_kafka_admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateTopicRequest(

    @NotBlank(message = "el nombre del tópico es obligatorio")
    @Pattern(regexp = Reglas.NOMBRE_REGEX, message = Reglas.NOMBRE_MENSAJE)
    String name,

    @NotNull(message = "el número de particiones es obligatorio")
    @Min(value = 1, message = "las particiones deben ser al menos 1")
    Integer partitions,

    @NotNull(message = "el factor de replicación es obligatorio")
    @Min(value = 1, message = "las réplicas deben ser al menos 1")
    @Max(value = 3, message = "las réplicas no pueden superar 3")
    Short replicationFactor,

    @Min(value = -1, message = "retentionMs debe ser mayor o igual a -1")
    Long retentionMs,

    @Pattern(
        regexp = "^(delete|compact|compact,delete|delete,compact)$",
        message = "cleanupPolicy debe ser delete, compact o compact,delete"
    )
    String cleanupPolicy
) {}