package cl.duoc.dsy1107.ms_kafka_admin.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.DisconnectException;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.NetworkException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.stereotype.Service;

import cl.duoc.dsy1107.ms_kafka_admin.dto.ConsumerGroupDetailResponse;
import cl.duoc.dsy1107.ms_kafka_admin.dto.ConsumerGroupDetailResponse.MemberDto;
import cl.duoc.dsy1107.ms_kafka_admin.dto.ConsumerGroupDetailResponse.PartitionOffsetDto;
import cl.duoc.dsy1107.ms_kafka_admin.dto.ConsumerGroupSummary;
import cl.duoc.dsy1107.ms_kafka_admin.dto.CreateTopicRequest;
import cl.duoc.dsy1107.ms_kafka_admin.dto.TopicDetailResponse;
import cl.duoc.dsy1107.ms_kafka_admin.dto.TopicDetailResponse.PartitionInfoDto;
import cl.duoc.dsy1107.ms_kafka_admin.dto.UpdateTopicConfigRequest;
import cl.duoc.dsy1107.ms_kafka_admin.exception.KafkaNoDisponibleException;
import cl.duoc.dsy1107.ms_kafka_admin.exception.RecursoNoEncontradoException;
import cl.duoc.dsy1107.ms_kafka_admin.exception.RecursoYaExisteException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KafkaAdminService {

    private final AdminClient adminClient;

    // --- TÓPICOS ---

    public List<String> listarTopics() {
        try {
            Set<String> names = adminClient.listTopics().names().get();
            return names.stream().sorted().toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaNoDisponibleException("Operación interrumpida al consultar Kafka", e);
        } catch (ExecutionException e) {
            throw traducirError(e, null);
        }
    }

    public TopicDetailResponse obtenerTopic(String name) {
        try {
            TopicDescription desc = adminClient.describeTopics(List.of(name))
                    .allTopicNames().get().get(name);

            if (desc == null) {
                throw new RecursoNoEncontradoException("El tópico no existe: " + name);
            }

            ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
            Config config = adminClient.describeConfigs(List.of(resource)).all().get().get(resource);

            List<PartitionInfoDto> partitions = desc.partitions().stream()
                    .map(p -> new PartitionInfoDto(
                            p.partition(),
                            p.leader() != null ? p.leader().id() : -1,
                            p.replicas().stream().map(Node::id).toList(),
                            p.isr().stream().map(Node::id).toList()))
                    .sorted(Comparator.comparingInt(PartitionInfoDto::partition))
                    .toList();

            Map<String, String> configs = new TreeMap<>();
            if (config != null) {
                for (ConfigEntry entry : config.entries()) {
                    if (!entry.isSensitive()) {
                        configs.put(entry.name(), entry.value());
                    }
                }
            }

            return new TopicDetailResponse(desc.name(), partitions.size(), partitions, configs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaNoDisponibleException("Operación interrumpida al consultar el tópico", e);
        } catch (ExecutionException e) {
            throw traducirError(e, name);
        }
    }

    public CreateTopicRequest crearTopic(CreateTopicRequest req) {
        validarNombreReservado(req.name());

        NewTopic newTopic = new NewTopic(req.name(), req.partitions(), req.replicationFactor());
        Map<String, String> configs = new HashMap<>();

        if (req.retentionMs() != null) {
            configs.put("retention.ms", String.valueOf(req.retentionMs()));
        }
        if (req.cleanupPolicy() != null && !req.cleanupPolicy().isBlank()) {
            configs.put("cleanup.policy", req.cleanupPolicy());
        }
        if (!configs.isEmpty()) {
            newTopic.configs(configs);
        }

        try {
            adminClient.createTopics(List.of(newTopic)).all().get();
            return req;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaNoDisponibleException("Operación interrumpida al crear el tópico", e);
        } catch (ExecutionException e) {
            throw traducirError(e, req.name());
        }
    }

    public TopicDetailResponse actualizarConfigTopic(String name, UpdateTopicConfigRequest req) {
        Map<String, String> cambios = new LinkedHashMap<>();

        if (req.configs() != null) {
            req.configs().forEach((k, v) -> {
                if (k != null && !k.isBlank() && v != null) {
                    cambios.put(k.trim(), v.trim());
                }
            });
        }
        if (req.retentionMs() != null) {
            cambios.put("retention.ms", String.valueOf(req.retentionMs()));
        }
        if (req.cleanupPolicy() != null && !req.cleanupPolicy().isBlank()) {
            cambios.put("cleanup.policy", req.cleanupPolicy().trim());
        }

        if (cambios.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos una configuración válida para actualizar");
        }

        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        Collection<AlterConfigOp> ops = new ArrayList<>();
        cambios.forEach((k, v) -> ops.add(new AlterConfigOp(new ConfigEntry(k, v), AlterConfigOp.OpType.SET)));

        try {
            adminClient.incrementalAlterConfigs(Map.of(resource, ops)).all().get();
            return obtenerTopic(name);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaNoDisponibleException("Operación interrumpida al actualizar el tópico", e);
        } catch (ExecutionException e) {
            throw traducirError(e, name);
        }
    }

    public void eliminarTopic(String name) {
        try {
            adminClient.deleteTopics(List.of(name)).all().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaNoDisponibleException("Operación interrumpida al eliminar el tópico", e);
        } catch (ExecutionException e) {
            throw traducirError(e, name);
        }
    }

    // --- CONSUMER GROUPS ---

    public List<ConsumerGroupSummary> listarConsumerGroups() {
        try {
            Collection<ConsumerGroupListing> groups = adminClient.listConsumerGroups().all().get();
            return groups.stream()
                    .map(g -> new ConsumerGroupSummary(
                            g.groupId(),
                            g.state().map(Enum::name).orElse("UNKNOWN")))
                    .sorted(Comparator.comparing(ConsumerGroupSummary::groupId))
                    .toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaNoDisponibleException("Operación interrumpida al listar consumer groups", e);
        } catch (ExecutionException e) {
            throw traducirError(e, null);
        }
    }

    public ConsumerGroupDetailResponse obtenerConsumerGroup(String groupId) {
        try {
            boolean existe = adminClient.listConsumerGroups().all().get().stream()
                    .anyMatch(g -> g.groupId().equals(groupId));

            if (!existe) {
                throw new RecursoNoEncontradoException("El consumer group no existe: " + groupId);
            }

            ConsumerGroupDescription desc = adminClient.describeConsumerGroups(List.of(groupId))
                    .all().get().get(groupId);

            List<MemberDto> members = desc.members().stream()
                    .map(this::mapearMiembro)
                    .toList();

            Map<TopicPartition, OffsetAndMetadata> committedOffsets = adminClient
                    .listConsumerGroupOffsets(groupId)
                    .partitionsToOffsetAndMetadata()
                    .get();

            List<PartitionOffsetDto> offsets = new ArrayList<>();
            if (!committedOffsets.isEmpty()) {
                Map<TopicPartition, OffsetSpec> latestSpecs = new HashMap<>();
                committedOffsets.keySet().forEach(tp -> latestSpecs.put(tp, OffsetSpec.latest()));

                Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets = adminClient
                        .listOffsets(latestSpecs)
                        .all()
                        .get();

                for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : committedOffsets.entrySet()) {
                    TopicPartition tp = entry.getKey();
                    long currentOffset = entry.getValue().offset();
                    ListOffsetsResult.ListOffsetsResultInfo endInfo = endOffsets.get(tp);
                    long endOffset = endInfo != null ? endInfo.offset() : currentOffset;
                    long lag = Math.max(0L, endOffset - currentOffset);

                    offsets.add(new PartitionOffsetDto(
                            tp.topic(), tp.partition(), currentOffset, endOffset, lag));
                }

                offsets.sort(Comparator.comparing(PartitionOffsetDto::topic)
                        .thenComparingInt(PartitionOffsetDto::partition));
            }

            return new ConsumerGroupDetailResponse(
                    desc.groupId(),
                    desc.state().name(),
                    members,
                    offsets);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaNoDisponibleException("Operación interrumpida al consultar el consumer group", e);
        } catch (ExecutionException e) {
            throw traducirError(e, groupId);
        }
    }

    // --- MÉTODOS AUXILIARES ---

    private MemberDto mapearMiembro(MemberDescription m) {
        List<String> asignaciones = m.assignment().topicPartitions().stream()
                .map(tp -> tp.topic() + "-" + tp.partition())
                .sorted()
                .toList();
        return new MemberDto(m.consumerId(), m.clientId(), m.host(), asignaciones);
    }

    private void validarNombreReservado(String name) {
        if (".".equals(name) || "..".equals(name)) {
            throw new IllegalArgumentException("El nombre del tópico no puede ser '.' ni '..'");
        }
    }

    private RuntimeException traducirError(ExecutionException e, String recurso) {
        Throwable cause = e.getCause();

        if (cause instanceof UnknownTopicOrPartitionException) {
            return new RecursoNoEncontradoException("El tópico no existe: " + recurso);
        }
        if (cause instanceof GroupIdNotFoundException) {
            return new RecursoNoEncontradoException("El consumer group no existe: " + recurso);
        }
        if (cause instanceof TopicExistsException) {
            return new RecursoYaExisteException("El tópico ya existe: " + recurso);
        }
        if (cause instanceof InvalidConfigurationException
                || cause instanceof InvalidTopicException
                || cause instanceof InvalidReplicationFactorException
                || cause instanceof InvalidPartitionsException) {
            return new IllegalArgumentException("Configuración inválida en Kafka: " + cause.getMessage());
        }
        if (cause instanceof TimeoutException
                || cause instanceof DisconnectException
                || cause instanceof NetworkException) {
            return new KafkaNoDisponibleException("No hay conexión con el clúster de Kafka", cause);
        }

        return new KafkaNoDisponibleException(
                "Error al comunicarse con Kafka: " + (cause != null ? cause.getMessage() : e.getMessage()),
                cause != null ? cause : e);
    }
}