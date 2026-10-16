package cl.duoc.dsy1107.biblioteca_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.duoc.dsy1107.biblioteca_backend.model.Libro;

public interface LibroRepository extends JpaRepository<Libro, Long>{
    
}
