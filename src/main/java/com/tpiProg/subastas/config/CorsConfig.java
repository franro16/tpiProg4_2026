package com.tpiProg.subastas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Origenes permitidos: localhost para desarrollo y frontend en produccion
        config.setAllowedOriginPatterns(List.of("*"));

        // Metodos HTTP permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Headers permitidos en el request
        config.setAllowedHeaders(List.of("*"));

        // Permite enviar cookies y el header Authorization con el JWT
        config.setAllowCredentials(true);

        // Cuanto tiempo el browser cachea la respuesta del preflight (en segundos)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}