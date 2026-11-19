package cl.duoc.dsy1107.ms_prestamo.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import cl.duoc.dsy1107.ms_prestamo.client.LibroClient;
import cl.duoc.dsy1107.ms_prestamo.client.LibroDTO;
import cl.duoc.dsy1107.ms_prestamo.event.PrestamoCreadoEvent;
import cl.duoc.dsy1107.ms_prestamo.event.PrestamoDevueltoEvent;
import cl.duoc.dsy1107.ms_prestamo.event.PrestamoEventPublisher;
import cl.duoc.dsy1107.ms_prestamo.kafka.PrestamoKafkaPublisher;
import cl.duoc.dsy1107.ms_prestamo.model.EstadoPrestamo;
import cl.duoc.dsy1107.ms_prestamo.model.Prestamo;
import cl.duoc.dsy1107.ms_prestamo.repository.PrestamoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrestamoService {

    private final PrestamoRepository prestamoRepository;
    private final LibroClient libroClient;
    private final PrestamoEventPublisher publisher;
    private final PrestamoKafkaPublisher kafkaPublisher;

    public Prestamo crearPrestamo(Long libroId, String usuario, String authorization) {

        //1. SINCRONO: reservar stock en ms-catalogo (devuelve el libro con su titulo)
        LibroDTO libro = reservarStock(libroId, authorization);

        //2. Guardar prestamo (si falla, se devuelve el stock reservado)
        Instant ahora = Instant.now();
        Prestamo prestamo = new Prestamo();
        prestamo.setLibroId(libroId);
        prestamo.setTitulo(libro.titulo());
        prestamo.setUsuario(usuario);
        prestamo.setFechaPrestamo(ahora);
        prestamo.setFechaDevolucionEstimada(ahora.plus(14, ChronoUnit.DAYS));
        prestamo.setEstado(EstadoPrestamo.ACTIVO);

        Prestamo guardado;
        try {
            guardado = prestamoRepository.save(prestamo);
        } catch (RuntimeException e) {
            liberarStockSilencioso(libroId, authorization);
            throw e;
        }

        //3. ASINCRONO: publicar por RabbitMQ
        // El mismo eventId viaja por RabbitMQ y por Kafka: ms-historial detecta el duplicado
        String eventId = UUID.randomUUID().toString();
        publisher.publicarCreado(new PrestamoCreadoEvent(
            eventId,
            "prestamo.creado",
            guardado.getId(),
            guardado.getLibroId(),
            guardado.getTitulo(),
            guardado.getUsuario(),
            guardado.getFechaPrestamo(),
            guardado.getFechaDevolucionEstimada(),
            guardado.getEstado().name()
        ));

        kafkaPublisher.publicarCreado(guardado, eventId);
        
        return guardado;
    }

    public Prestamo devolverPrestamo(Long id, String usuario, String authorization) {
        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Préstamo no encontrado"));

        if (!prestamo.getUsuario().equals(usuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este préstamo pertenece a otro usuario");
        }

        if (prestamo.getEstado() == EstadoPrestamo.DEVUELTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El préstamo ya fue devuelto");
        }

        // SINCRONO: devolver el stock antes de marcar el préstamo como devuelto
        liberarStock(prestamo.getLibroId(), authorization);

        prestamo.setEstado(EstadoPrestamo.DEVUELTO);
        prestamo.setFechaDevolucionReal(Instant.now());
        Prestamo guardado = prestamoRepository.save(prestamo);

        String eventId = UUID.randomUUID().toString();
        publisher.publicarDevuelto(new PrestamoDevueltoEvent(
            eventId,
            "prestamo.devuelto",
            guardado.getId(),
            guardado.getLibroId(),
            guardado.getTitulo(),
            guardado.getUsuario(),
            guardado.getFechaDevolucionReal(),
            guardado.getEstado().name()
        ));

        kafkaPublisher.publicarDevuelto(guardado, eventId);

        return guardado;
    }

    public List<Prestamo> listarPrestamosDe(String usuario) {
        return prestamoRepository.findByUsuarioOrderByFechaPrestamoDesc(usuario);
    }

    private LibroDTO reservarStock(Long libroId, String authorization) {
        try {
            return libroClient.reservarStock(libroId, authorization);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El libro no existe");
        } catch (HttpClientErrorException.Conflict e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Libro sin stock");
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ms-catalogo no está disponible");
        }
    }

    private void liberarStock(Long libroId, String authorization) {
        try {
            libroClient.liberarStock(libroId, authorization);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ms-catalogo no está disponible");
        }
    }

    private void liberarStockSilencioso(Long libroId, String authorization) {
        try {
            libroClient.liberarStock(libroId, authorization);
        } catch (RestClientException ignorada) {
        }
    }
}