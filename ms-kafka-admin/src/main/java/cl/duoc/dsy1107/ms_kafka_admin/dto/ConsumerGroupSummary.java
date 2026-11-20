package cl.duoc.dsy1107.ms_kafka_admin.dto;

public record ConsumerGroupSummary(
    String groupId,
    String state
) {}