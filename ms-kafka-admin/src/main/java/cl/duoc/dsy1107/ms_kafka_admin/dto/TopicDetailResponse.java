package cl.duoc.dsy1107.ms_kafka_admin.dto;

import java.util.List;
import java.util.Map;

public record TopicDetailResponse(
    String name,
    int partitionCount,
    List<PartitionInfoDto> partitions,
    Map<String, String> configs
) {
    public record PartitionInfoDto(
        int partition,
        int leader,
        List<Integer> replicas,
        List<Integer> isr
    ) {}
}