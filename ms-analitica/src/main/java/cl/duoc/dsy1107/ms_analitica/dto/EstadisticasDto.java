package cl.duoc.dsy1107.ms_analitica.dto;

import java.util.List;

public record EstadisticasDto(
    long totalPrestamos,
    long totalDevoluciones,
    long prestamosActivos,
    List<LibroEstadisticaDto> masPrestados,
    List<LibroEstadisticaDto> sinStock
) {}