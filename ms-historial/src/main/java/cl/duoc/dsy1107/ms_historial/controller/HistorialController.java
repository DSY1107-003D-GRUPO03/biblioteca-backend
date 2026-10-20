package cl.duoc.dsy1107.ms_historial.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.dsy1107.ms_historial.dto.EntradaHistorial;
import cl.duoc.dsy1107.ms_historial.service.HistorialService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/historial")
@RequiredArgsConstructor
public class HistorialController {

    private final HistorialService historialService;

    @GetMapping
    public List<EntradaHistorial> listar(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String usuario) {
        return historialService.listar(tipo, usuario);
    }
}
