package cl.duoc.dsy1107.biblioteca_backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.dsy1107.biblioteca_backend.exception.RecursoNoEncontradoException;
import cl.duoc.dsy1107.biblioteca_backend.model.Libro;
import cl.duoc.dsy1107.biblioteca_backend.repository.LibroRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LibroService {
    
    private final LibroRepository libroRepository;

    @Transactional(readOnly = true)
    public List<Libro> obtenerLibros() {
        return libroRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Libro obtenerPorId(Long id) {
        return libroRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el libro con id: " + id));
    }

    @Transactional
    public Libro crearLibro(Libro libro) {
        return libroRepository.save(libro);
    }

    @Transactional
    public void eliminarLibro(Long id) {
        libroRepository.delete(obtenerPorId(id));
    }

}
