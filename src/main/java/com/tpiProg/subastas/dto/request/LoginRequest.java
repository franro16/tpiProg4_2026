package com.tpiProg.subastas.dto.request;

import jakarta.validation.constraints.NotBlank;

// Datos para iniciar sesión
public record LoginRequest(
        @NotBlank(message = "El email o usuario es obligatorio")
        String identifier,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {}