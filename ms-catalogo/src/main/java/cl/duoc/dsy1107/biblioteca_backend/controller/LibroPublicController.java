package cl.duoc.dsy1107.biblioteca_backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.dsy1107.biblioteca_backend.model.Libro;
import cl.duoc.dsy1107.biblioteca_backend.service.LibroService;
import lombok.RequiredArgsConstructor;



@RestController
@RequestMapping("/public/libros")
@RequiredArgsConstructor
public class LibroPublicController {
    
    private final LibroService libroService;

    @GetMapping
    public List<Libro> listar() {
        return libroService.obtenerLibros();
    }

}
