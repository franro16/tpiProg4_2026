package com.tpiProg.subastas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            log.info("--- INICIANDO FILTRO JWT ---");
            String jwt = extractToken(request);
            
            if (jwt == null) {
                log.warn("El token JWT es nulo. ¿Se envió el header Authorization?");
            } else {
                log.info("Token extraído correctamente. Validando...");
                
                if (jwtTokenProvider.validateToken(jwt)) {
                    log.info("Token válido. Extrayendo email...");
                    String email = jwtTokenProvider.getEmailFromToken(jwt);
                    log.info("Email extraído: {}", email);

                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
                    log.info("Usuario encontrado en BD: {}", userDetails.getUsername());

                    if (userDetails.isAccountNonLocked()) {
                        log.info("La cuenta NO está bloqueada. Autenticando usuario...");
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.info("Autenticación exitosa. Roles asignados: {}", userDetails.getAuthorities());
                    } else {
                        log.warn("¡ATENCIÓN! La cuenta del usuario está bloqueada en la BD.");
                    }
                } else {
                    log.warn("¡ATENCIÓN! La validación del token falló (validateToken devolvió false).");
                }
            }
        } catch (Exception ex) {
            log.error("Excepción al intentar autenticar: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}