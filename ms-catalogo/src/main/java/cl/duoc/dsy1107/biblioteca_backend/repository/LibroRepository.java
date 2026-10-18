package cl.duoc.dsy1107.biblioteca_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cl.duoc.dsy1107.biblioteca_backend.model.Libro;

public interface LibroRepository extends JpaRepository<Libro, Long>{
    
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query ("update Libro l set l.stock = l.stock - 1 where l.id = :id and l.stock > 0")
    int descontarStock(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Libro l set l.stock = l.stock + 1 where l.id = :id")
    int sumarStock(@Param("id") Long id);

    boolean existsByIsbn(String isbn);
}
