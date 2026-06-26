package com.tpiProg.subastas.service;

import com.tpiProg.subastas.dto.request.LoginRequest;
import com.tpiProg.subastas.dto.request.RegisterRequest;
import com.tpiProg.subastas.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}