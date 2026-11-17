package cl.duoc.dsy1107.ms_analitica.service;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.dsy1107.ms_analitica.dto.EstadisticasDto;
import cl.duoc.dsy1107.ms_analitica.dto.LibroEstadisticaDto;
import cl.duoc.dsy1107.ms_analitica.event.CatalogoEventoDto;
import cl.duoc.dsy1107.ms_analitica.event.PrestamoEventoDto;
import cl.duoc.dsy1107.ms_analitica.model.EstadisticaLibro;
import cl.duoc.dsy1107.ms_analitica.model.EventoProcesado;
import cl.duoc.dsy1107.ms_analitica.repository.EstadisticaLibroRepository;
import cl.duoc.dsy1107.ms_analitica.repository.EventoProcesadoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstadisticasService {

    private static final Logger log = LoggerFactory.getLogger(EstadisticasService.class);

    private static final Set<String> TIPOS_PRESTAMO = Set.of("CREADO", "DEVUELTO");
    private static final Set<String> TIPOS_CATALOGO = Set.of("CREADO", "ACTUALIZADO", "ELIMINADO", "STOCK_AGOTADO");

    private final EstadisticaLibroRepository estadisticaRepository;
    private final EventoProcesadoRepository eventoRepository;

    @Transactional
    public void registrarPrestamo(PrestamoEventoDto evento) {
        if (evento == null) {
            log.warn("Mensaje de préstamo sin contenido, se ignora");
            return;
        }
        if (esInvalido(evento.eventId(), evento.libroId(), evento.tipo(), TIPOS_PRESTAMO)) {
            throw new IllegalArgumentException("Evento de préstamo inválido: " + evento);
        }
        if (eventoRepository.existsById(evento.eventId())) {
            log.info("Evento {} ya procesado, se ignora", evento.eventId());
            return;
        }

        EstadisticaLibro estadistica = obtenerOCrear(evento.libroId());
        if (evento.titulo() != null) {
            estadistica.setTitulo(evento.titulo());
        }
        if ("CREADO".equals(evento.tipo())) {
            estadistica.setPrestamos(estadistica.getPrestamos() + 1);
        } else {
            estadistica.setDevoluciones(estadistica.getDevoluciones() + 1);
        }

        estadisticaRepository.save(estadistica);
        eventoRepository.save(new EventoProcesado(evento.eventId()));
        log.info("Evento de préstamo {} ({}) procesado para el libro {}",
                evento.eventId(), evento.tipo(), evento.libroId());
    }

    @Transactional
    public void registrarCatalogo(CatalogoEventoDto evento) {
        if (evento == null) {
            log.warn("Mensaje de catálogo sin contenido, se ignora");
            return;
        }
        if (esInvalido(evento.eventId(), evento.libroId(), evento.tipo(), TIPOS_CATALOGO)) {
            throw new IllegalArgumentException("Evento de catálogo inválido: " + evento);
        }
        if (eventoRepository.existsById(evento.eventId())) {
            log.info("Evento {} ya procesado, se ignora", evento.eventId());
            return;
        }

        EstadisticaLibro estadistica = obtenerOCrear(evento.libroId());
        if (evento.titulo() != null) {
            estadistica.setTitulo(evento.titulo());
        }
        if ("ELIMINADO".equals(evento.tipo())) {
            estadistica.setEliminado(true);
        } else {
            estadistica.setEliminado(false);
            estadistica.setStock(evento.stock());
        }

        estadisticaRepository.save(estadistica);
        eventoRepository.save(new EventoProcesado(evento.eventId()));
        log.info("Evento de catálogo {} ({}) procesado para el libro {}",
                evento.eventId(), evento.tipo(), evento.libroId());
    }

    @Transactional(readOnly = true)
    public EstadisticasDto obtenerResumen() {
        long prestamos = estadisticaRepository.totalPrestamos();
        long devoluciones = estadisticaRepository.totalDevoluciones();

        List<LibroEstadisticaDto> masPrestados = estadisticaRepository
                .findTop5ByOrderByPrestamosDescTituloAsc().stream().map(this::aDto).toList();
        List<LibroEstadisticaDto> sinStock = estadisticaRepository
                .findByStockAndEliminadoFalse(0).stream().map(this::aDto).toList();

        return new EstadisticasDto(prestamos, devoluciones, prestamos - devoluciones, masPrestados, sinStock);
    }

    private boolean esInvalido(String eventId, Long libroId, String tipo, Set<String> tiposValidos) {
        return eventId == null || eventId.isBlank() || libroId == null || !tiposValidos.contains(tipo);
    }

    private EstadisticaLibro obtenerOCrear(Long libroId) {
        return estadisticaRepository.findById(libroId).orElseGet(() -> {
            EstadisticaLibro nueva = new EstadisticaLibro();
            nueva.setLibroId(libroId);
            return nueva;
        });
    }

    private LibroEstadisticaDto aDto(EstadisticaLibro e) {
        return new LibroEstadisticaDto(e.getLibroId(), e.getTitulo(), e.getPrestamos(), e.getDevoluciones(), e.getStock());
    }
}