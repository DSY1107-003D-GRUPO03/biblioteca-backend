package cl.duoc.dsy1107.ms_analitica.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import cl.duoc.dsy1107.ms_analitica.model.EstadisticaLibro;

public interface EstadisticaLibroRepository extends JpaRepository<EstadisticaLibro, Long> {

    List<EstadisticaLibro> findTop5ByOrderByPrestamosDescTituloAsc();

    List<EstadisticaLibro> findByStockAndEliminadoFalse(Integer stock);

    @Query("select coalesce(sum(e.prestamos), 0) from EstadisticaLibro e")
    long totalPrestamos();

    @Query("select coalesce(sum(e.devoluciones), 0) from EstadisticaLibro e")
    long totalDevoluciones();
}