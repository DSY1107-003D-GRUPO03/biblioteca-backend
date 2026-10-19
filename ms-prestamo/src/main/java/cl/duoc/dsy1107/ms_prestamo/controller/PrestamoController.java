package cl.duoc.dsy1107.ms_prestamo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.dsy1107.ms_prestamo.model.Prestamo;
import cl.duoc.dsy1107.ms_prestamo.service.PrestamoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PrestamoController {

    private final PrestamoService prestamoService;

    public record CrearPrestamoRequest(@NotNull(message = "libroId es obligatorio") Long libroId) {}

    @PostMapping("/write/prestamos")
    @ResponseStatus(HttpStatus.CREATED)
    public Prestamo crear(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CrearPrestamoRequest request) {

        return prestamoService.crearPrestamo(
                request.libroId(),
                jwt.getSubject(),
                bearer(jwt));
    }

    @PostMapping("/write/prestamos/{id}/devolucion")
    public Prestamo devolver(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        return prestamoService.devolverPrestamo(id, jwt.getSubject(), bearer(jwt));
    }

    @GetMapping("/prestamos")
    public List<Prestamo> listar(@AuthenticationPrincipal Jwt jwt) {
        return prestamoService.listarPrestamosDe(jwt.getSubject());
    }

    private String bearer(Jwt jwt) {
        return "Bearer " + jwt.getTokenValue();
    }
}