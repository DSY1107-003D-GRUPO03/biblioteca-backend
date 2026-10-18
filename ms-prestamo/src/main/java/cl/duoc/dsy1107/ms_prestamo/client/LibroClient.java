package cl.duoc.dsy1107.ms_prestamo.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class LibroClient {

    private final RestClient restClient;

    public LibroClient(@Value("${catalogo.url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public LibroDTO consultarLibro(Long libroId, String authorization) {
        return restClient.get()
                .uri("/api/libros/{id}", libroId)
                .header("Authorization", authorization)
                .retrieve()
                .body(LibroDTO.class);
    }
}

