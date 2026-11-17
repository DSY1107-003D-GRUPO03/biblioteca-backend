package cl.duoc.dsy1107.ms_analitica.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "estadistica_libro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstadisticaLibro {

    @Id
    private Long libroId;

    private String titulo;

    @Column(nullable = false)
    private long prestamos;

    @Column(nullable = false)
    private long devoluciones;

    private Integer stock;

    @Column(nullable = false)
    private boolean eliminado;
}