package cl.duoc.dsy1107.ms_analitica.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.dsy1107.ms_analitica.dto.EstadisticasDto;
import cl.duoc.dsy1107.ms_analitica.service.EstadisticasService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/estadisticas")
@RequiredArgsConstructor
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    @GetMapping
    public EstadisticasDto obtener() {
        return estadisticasService.obtenerResumen();
    }
}