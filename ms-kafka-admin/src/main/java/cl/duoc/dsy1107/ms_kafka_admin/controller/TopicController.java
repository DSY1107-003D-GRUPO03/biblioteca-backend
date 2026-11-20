package cl.duoc.dsy1107.ms_kafka_admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.dsy1107.ms_kafka_admin.dto.CreateTopicRequest;
import cl.duoc.dsy1107.ms_kafka_admin.dto.Reglas;
import cl.duoc.dsy1107.ms_kafka_admin.dto.TopicDetailResponse;
import cl.duoc.dsy1107.ms_kafka_admin.dto.UpdateTopicConfigRequest;
import cl.duoc.dsy1107.ms_kafka_admin.service.KafkaAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/topics")
@RequiredArgsConstructor
public class TopicController {

    private final KafkaAdminService kafkaAdminService;

    @GetMapping
    public List<String> listar() {
        return kafkaAdminService.listarTopics();
    }

    @GetMapping("/{name}")
    public TopicDetailResponse obtener(
            @PathVariable
            @Pattern(regexp = Reglas.NOMBRE_REGEX, message = Reglas.NOMBRE_MENSAJE)
            String name) {
        return kafkaAdminService.obtenerTopic(name);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateTopicRequest crear(@Valid @RequestBody CreateTopicRequest request) {
        return kafkaAdminService.crearTopic(request);
    }

    @PutMapping("/{name}/config")
    public TopicDetailResponse actualizarConfig(
            @PathVariable
            @Pattern(regexp = Reglas.NOMBRE_REGEX, message = Reglas.NOMBRE_MENSAJE)
            String name,
            @Valid @RequestBody UpdateTopicConfigRequest request) {
        return kafkaAdminService.actualizarConfigTopic(name, request);
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @PathVariable
            @Pattern(regexp = Reglas.NOMBRE_REGEX, message = Reglas.NOMBRE_MENSAJE)
            String name) {
        kafkaAdminService.eliminarTopic(name);
    }
}