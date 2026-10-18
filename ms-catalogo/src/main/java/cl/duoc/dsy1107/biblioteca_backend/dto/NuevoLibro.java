package cl.duoc.dsy1107.biblioteca_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NuevoLibro(

    @NotBlank(message = "el titulo es obligatorio")
    @Size(max = 200, message = "el titulo no puede superar los 200 caracteres")
    String titulo,

    @NotBlank(message = "el autor es obligatorio")
    @Size(max = 150, message = "el autor no puede superar los 150 caracteres")
    String autor,

    @NotBlank(message = "el ISBN es obligatorio")
    @Pattern (regexp = "^[0-9-]{10,17}$", message = "el ISBN debe tener entre 10 y 17 dígitos o guiones")
    String isbn,

    @NotNull(message = "el stock es obligatorio")
    @Min(value = 0, message = "debe ser mayor o igual a 0")
    Integer stock) {

    }
