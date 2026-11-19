package cl.duoc.dsy1107.ms_kafka_admin.dto;

import java.util.List;

public record ConsumerGroupDetailResponse(
    String groupId,
    String state,
    List<MemberDto> members,
    List<PartitionOffsetDto> offsets
) {
    public record MemberDto(
        String memberId,
        String clientId,
        String host,
        List<String> assignments
    ) {}

    public record PartitionOffsetDto(
        String topic,
        int partition,
        long currentOffset,
        long endOffset,
        long lag
    ) {}
}