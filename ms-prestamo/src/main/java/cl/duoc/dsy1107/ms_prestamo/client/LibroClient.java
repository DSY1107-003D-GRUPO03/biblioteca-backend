package cl.duoc.dsy1107.ms_prestamo.client;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class LibroClient {

    private final RestClient restClient;

    public LibroClient(@Value("${catalogo.url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public LibroDTO consultarLibro(Long libroId, String authorization) {
        return restClient.get()
                .uri("/api/libros/{id}", libroId)
                .header("Authorization", authorization)
                .retrieve()
                .body(LibroDTO.class);
    }

    public LibroDTO reservarStock(Long libroId, String authorization) {
        return restClient.post()
                .uri("/api/write/libros/{id}/stock/reservar", libroId)
                .header("Authorization", authorization)
                .retrieve()
                .body(LibroDTO.class);
    }

    public LibroDTO liberarStock(Long libroId, String authorization) {
        return restClient.post()
                .uri("/api/write/libros/{id}/stock/liberar", libroId)
                .header("Authorization", authorization)
                .retrieve()
                .body(LibroDTO.class);
    }
}