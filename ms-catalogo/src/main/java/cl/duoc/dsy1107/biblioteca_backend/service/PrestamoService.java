package cl.duoc.dsy1107.biblioteca_backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.dsy1107.biblioteca_backend.exception.SinStockException;
import cl.duoc.dsy1107.biblioteca_backend.model.Libro;
import cl.duoc.dsy1107.biblioteca_backend.model.Prestamo;
import cl.duoc.dsy1107.biblioteca_backend.repository.PrestamoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrestamoService {
    
    private final PrestamoRepository prestamoRepository;
    private final LibroService libroService;

    @Transactional(readOnly = true)
    public List<Prestamo> obtenerPorUsuario(String usuario) {
        return prestamoRepository.findByUsuario(usuario);
    }

    @Transactional
    public Prestamo crearPrestamo(Long libroId, String usuario) {
        Libro libro = libroService.obtenerPorId(libroId);

        if ( libro.getStock() == null || libro.getStock() <= 0 ) {
            throw new SinStockException("No hay stock disponible para el libro: " + libro.getTitulo());
        }

        libro.setStock(libro.getStock() - 1);

        Prestamo prestamo = new Prestamo(libro,usuario);
        return prestamoRepository.save(prestamo);

    }

}
