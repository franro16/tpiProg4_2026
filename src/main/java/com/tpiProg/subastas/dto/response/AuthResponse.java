package com.tpiProg.subastas.dto.response;

// Lo que le devolvemos al usuario cuando inicia sesión correctamente
public record AuthResponse(
        String token, // Acá irá el JWT que armará tu compañero
        String username,
        String role
) {}