package cl.duoc.dsy1107.biblioteca_backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.dsy1107.biblioteca_backend.model.Libro;
import cl.duoc.dsy1107.biblioteca_backend.service.LibroService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;


@RestController 
@RequestMapping("/api/write/libros")
@RequiredArgsConstructor 
public class LibroStockController {
    
    private final LibroService libroService;

    @PostMapping("/{id}/stock/reservar")
    public Libro reservar(@PathVariable Long id) {
        return libroService.reservarStock(id);
    }
    
    @PostMapping("/{id}/stock/liberar")
    public Libro liberar(@PathVariable Long id) {
        return libroService.liberarStock(id);
    }

}
