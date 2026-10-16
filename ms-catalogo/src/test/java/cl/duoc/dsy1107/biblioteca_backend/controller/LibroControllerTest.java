package cl.duoc.dsy1107.biblioteca_backend.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest 
@AutoConfigureMockMvc 
@ActiveProfiles("test")
public class LibroControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @Test 
    void detalleLibroSinTokenResponder401() throws Exception {
        mockMvc.perform(get("/api/libros/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void gestionCatalogoConTokenSinRolAdminResponder403() throws Exception {
        mockMvc.perform(post("/api/admin/libros")
                        .with(jwt())
                        .contentType("application/json")
                        .content("{\"titulo\":\"Test\",\"autor\":\"Test\",\"isbn\":\"123\",\"stock\":1}"))
                .andExpect(status().isForbidden());
    }

}
