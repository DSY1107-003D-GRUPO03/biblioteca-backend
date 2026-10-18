package cl.duoc.dsy1107.ms_rabbit_admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.dsy1107.ms_rabbit_admin.dto.ExchangeRequest;
import cl.duoc.dsy1107.ms_rabbit_admin.dto.Reglas;
import cl.duoc.dsy1107.ms_rabbit_admin.service.RabbitAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/exchanges")
@RequiredArgsConstructor
public class ExchangeController {

    private final RabbitAdminService rabbitAdminService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExchangeRequest crear(@Valid @RequestBody ExchangeRequest request) {
        return rabbitAdminService.crearExchange(request);
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @PathVariable
            @Pattern(regexp = Reglas.NOMBRE_REGEX, message = Reglas.NOMBRE_MENSAJE)
            String name) {
        rabbitAdminService.eliminarExchange(name);
    }
}