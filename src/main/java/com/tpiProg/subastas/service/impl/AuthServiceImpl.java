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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // Maximos intentos antes de bloquear temporalmente
    private static final int MAX_FAILED_ATTEMPTS = 5;
    // Minutos de bloqueo temporal
    private static final int LOCK_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Ya existe un usuario con ese email");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Ya existe un usuario con ese nombre de usuario");
        }

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
                .orElseThrow(() -> new BusinessException(
                        "Rol no encontrado en la base de datos: " + roleType));

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .isBlocked(false)
                .failedAttempts(0)
                .roles(Set.of(role))
                .build();

        userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        String token = jwtTokenProvider.generateToken(authentication);
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return new AuthResponse(token, user.getUsername(), roles);
    }

    // ACA ESTA LA MAGIA: Le decimos que NO deshaga los cambios si lanzamos estas excepciones
    @Override
    @Transactional(noRollbackFor = {BusinessException.class, LockedException.class})
    public AuthResponse login(LoginRequest request) {
        String email = resolveEmail(request.identifier());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Credenciales invalidas"));

        // Verificar bloqueo permanente por ADMIN
        if (user.isBlocked()) {
            throw new LockedException("Usuario bloqueado por el administrador.");
        }

        // Verificar bloqueo temporal por intentos fallidos
        if (user.getLockedUntil() != null &&
                OffsetDateTime.now(ZoneOffset.UTC).isBefore(user.getLockedUntil())) {
            long minutosRestantes = java.time.Duration.between(
                    OffsetDateTime.now(ZoneOffset.UTC), user.getLockedUntil()).toMinutes() + 1;
            throw new LockedException(
                    "Cuenta bloqueada temporalmente. Intentá de nuevo en "
                            + minutosRestantes + " minuto(s).");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );

            // Login exitoso: resetear contador de intentos fallidos
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);

            String token = jwtTokenProvider.generateToken(authentication);
            String roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));

            return new AuthResponse(token, user.getUsername(), roles);

        } catch (BadCredentialsException e) {
            // Incrementar contador de intentos fallidos
            int intentos = user.getFailedAttempts() + 1;
            user.setFailedAttempts(intentos);

            if (intentos >= MAX_FAILED_ATTEMPTS) {
                // Bloquear temporalmente por 15 minutos
                user.setLockedUntil(
                        OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(LOCK_DURATION_MINUTES));
                userRepository.save(user);
                throw new LockedException(
                        "Demasiados intentos fallidos. Cuenta bloqueada por "
                                + LOCK_DURATION_MINUTES + " minutos.");
            }

            userRepository.save(user);
            int intentosRestantes = MAX_FAILED_ATTEMPTS - intentos;
            throw new BusinessException(
                    "Credenciales invalidas. Te quedan " + intentosRestantes + " intento(s).");
        }
    }

    private String resolveEmail(String identifier) {
        if (identifier.contains("@")) {
            return identifier;
        }
        return userRepository.findByUsername(identifier)
                .map(User::getEmail)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado: " + identifier));
    }
}