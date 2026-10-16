package cl.duoc.dsy1107.biblioteca_backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.dsy1107.biblioteca_backend.model.Libro;
import cl.duoc.dsy1107.biblioteca_backend.service.LibroService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/api/libros")
@RequiredArgsConstructor
public class LibroController {
    
    private final LibroService libroService;

    @GetMapping("/{id}")
    public Libro obtener(@PathVariable Long id) {
        return libroService.obtenerPorId(id);
    }
    

}
