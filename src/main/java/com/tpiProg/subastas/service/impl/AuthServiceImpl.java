package com.tpiProg.subastas.service.impl;

import com.tpiProg.subastas.domain.entity.Role;
import com.tpiProg.subastas.domain.entity.User;
import com.tpiProg.subastas.domain.enums.RoleType;
import com.tpiProg.subastas.dto.request.LoginRequest;
import com.tpiProg.subastas.dto.request.RegisterRequest;
import com.tpiProg.subastas.dto.response.AuthResponse;
import com.tpiProg.subastas.exception.BusinessException;
import com.tpiProg.subastas.repository.RoleRepository;
import com.tpiProg.subastas.repository.UserRepository;
import com.tpiProg.subastas.security.JwtTokenProvider;
import com.tpiProg.subastas.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // Validar duplicados
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Ya existe un usuario con ese email");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Ya existe un usuario con ese nombre de usuario");
        }

        // Validar y obtener el rol pedido (solo USER o SELLER permitidos en registro)
        RoleType roleType;
        try {
            roleType = RoleType.valueOf(request.role().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Rol invalido. Valores permitidos: USER, SELLER");
        }

        if (roleType == RoleType.ADMIN) {
            throw new BusinessException("No se puede registrar un usuario con rol ADMIN");
        }

        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new BusinessException("Rol no encontrado en la base de datos: " + roleType));

        // Construir y guardar el usuario
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .isBlocked(false)
                .roles(Set.of(role))
                .build();

        userRepository.save(user);

        // Autenticar para generar el token directamente tras el registro
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        String token = jwtTokenProvider.generateToken(authentication);

        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return new AuthResponse(token, user.getUsername(), roles);
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        // identifier puede ser email o username
        // Buscamos primero por email, luego por username
        String email = resolveEmail(request.identifier());

        // AuthenticationManager verifica password y estado de la cuenta (bloqueado, etc.)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        String token = jwtTokenProvider.generateToken(authentication);

        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        // Recuperamos username para la respuesta
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        return new AuthResponse(token, user.getUsername(), roles);
    }

    // Resuelve si el identifier es email o username y devuelve el email
    private String resolveEmail(String identifier) {
        if (identifier.contains("@")) {
            return identifier;
        }
        return userRepository.findByUsername(identifier)
                .map(User::getEmail)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado: " + identifier));
    }
}