package cl.duoc.dsy1107.ms_kafka_admin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.dsy1107.ms_kafka_admin.dto.ConsumerGroupDetailResponse;
import cl.duoc.dsy1107.ms_kafka_admin.dto.ConsumerGroupSummary;
import cl.duoc.dsy1107.ms_kafka_admin.dto.Reglas;
import cl.duoc.dsy1107.ms_kafka_admin.service.KafkaAdminService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/consumer-groups")
@RequiredArgsConstructor
public class ConsumerGroupController {

    private final KafkaAdminService kafkaAdminService;

    @GetMapping
    public List<ConsumerGroupSummary> listar() {
        return kafkaAdminService.listarConsumerGroups();
    }

    @GetMapping("/{id}")
    public ConsumerGroupDetailResponse obtener(
            @PathVariable
            @Pattern(regexp = Reglas.NOMBRE_REGEX, message = Reglas.NOMBRE_MENSAJE)
            String id) {
        return kafkaAdminService.obtenerConsumerGroup(id);
    }
}