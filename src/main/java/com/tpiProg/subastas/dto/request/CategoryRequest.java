package com.tpiProg.subastas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Lo que nos mandan para crear una categoría nueva
public record CategoryRequest(
        @NotBlank(message = "El nombre de la categoría es obligatorio")
        String name,

        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String description
) {}