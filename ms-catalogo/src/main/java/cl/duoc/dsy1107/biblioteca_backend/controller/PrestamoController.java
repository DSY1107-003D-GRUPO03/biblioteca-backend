package cl.duoc.dsy1107.biblioteca_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.dsy1107.biblioteca_backend.dto.NuevoPrestamoRequest;
import cl.duoc.dsy1107.biblioteca_backend.model.Prestamo;
import cl.duoc.dsy1107.biblioteca_backend.service.PrestamoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PrestamoController {
    
    private final PrestamoService prestamoService;

    @GetMapping("/api/prestamos")
    public List<Prestamo> misPrestamos(Authentication authentication) {
        return prestamoService.obtenerPorUsuario(authentication.getName());
    }

    @PostMapping("/api/write/prestamos")
    @ResponseStatus(HttpStatus.CREATED)
    public Prestamo solicitar(Authentication authentication, @RequestBody NuevoPrestamoRequest request) {
        return prestamoService.crearPrestamo(request.libroId(), authentication.getName());
    }

}
