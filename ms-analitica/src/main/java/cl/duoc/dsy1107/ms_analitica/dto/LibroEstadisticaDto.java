package cl.duoc.dsy1107.ms_analitica.dto;

public record LibroEstadisticaDto(
    Long libroId,
    String titulo,
    long prestamos,
    long devoluciones,
    Integer stock
) {}