package cl.duoc.dsy1107.ms_historial.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name = "historial_entry")
@Getter
@Setter 
@NoArgsConstructor 
public class HistorialEntry {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String eventId;

    private String tipoEvento;
    private Long prestamoId;
    private Long libroId;
    private String usuario;
    private Instant fecha;
    private Instant recibidoEn;
}
