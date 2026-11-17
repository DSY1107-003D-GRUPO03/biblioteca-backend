package cl.duoc.dsy1107.ms_analitica.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "evento_procesado")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EventoProcesado {

    @Id
    private String eventId;
}