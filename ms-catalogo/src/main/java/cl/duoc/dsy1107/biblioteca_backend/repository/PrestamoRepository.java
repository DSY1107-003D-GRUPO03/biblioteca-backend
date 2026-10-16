package cl.duoc.dsy1107.biblioteca_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.dsy1107.biblioteca_backend.model.Prestamo;
import java.util.List;


public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
    
    List<Prestamo> findByUsuario(String usuario);

}
