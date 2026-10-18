package cl.duoc.dsy1107.ms_rabbit_admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.dsy1107.ms_rabbit_admin.dto.BindingRequest;
import cl.duoc.dsy1107.ms_rabbit_admin.service.RabbitAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/bindings")
@RequiredArgsConstructor
public class BindingController {

    private final RabbitAdminService rabbitAdminService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BindingRequest crear(@Valid @RequestBody BindingRequest request) {
        rabbitAdminService.crearBinding(request);
        return request;
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@Valid @RequestBody BindingRequest request) {
        rabbitAdminService.eliminarBinding(request);
    }
}