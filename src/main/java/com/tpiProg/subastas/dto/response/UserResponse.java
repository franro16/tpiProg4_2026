package com.tpiProg.subastas.dto.response;

import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String email,
        boolean isBlocked,
        Set<String> roles
) {}