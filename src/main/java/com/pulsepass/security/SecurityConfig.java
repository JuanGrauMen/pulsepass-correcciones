package com.pulsepass.security;

import tools.jackson.databind.ObjectMapper;
import com.pulsepass.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
            JwtAuthenticationFilter jwtFilter,
            ObjectMapper objectMapper) {
        this.jwtFilter = jwtFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(
                                        response,
                                        HttpStatus.UNAUTHORIZED,
                                        "Unauthorized"
                                ))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(
                                        response,
                                        HttpStatus.FORBIDDEN,
                                        "Forbidden"
                                )))

                .authorizeHttpRequests(auth -> auth

                        // Registro público e inicio de sesión
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/users",
                                "/api/auth/login"
                        ).permitAll()

                        // Venues públicos
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/venues/active",
                                "/api/venues/*"
                        ).permitAll()

                        // Artistas públicos
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/artists/active",
                                "/api/artists/by-stage-name",
                                "/api/artists/*"
                        ).permitAll()

                        // Consultas públicas de eventos
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/events/published",
                                "/api/events/by-artist"
                        ).permitAll()

                        // El listado de tickets pagados es exclusivo de STAFF/ADMIN
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/events/*/tickets/paid"
                        ).hasAnyRole("STAFF", "ADMIN")

                        // Operaciones administrativas sobre eventos
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/events"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/events/*/publish"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/events/*/artists/*"
                        ).hasRole("ADMIN")

                        // El controlador restringe los eventos no publicados
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/events/*"
                        ).permitAll()

                        // Compra de tickets
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/tickets"
                        ).hasRole("USER")

                        // Consulta de tickets por correo: propietario o ADMIN
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/tickets/by-user"
                        ).hasAnyRole("USER", "ADMIN")

                        // Consulta de un ticket: propiedad o rol operativo
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/tickets/*"
                        ).hasAnyRole("USER", "STAFF", "ADMIN")

                        // Cancelación: USER propietario o ADMIN
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/tickets/*/cancel"
                        ).hasAnyRole("USER", "ADMIN")

                        // Uso de tickets: STAFF/ADMIN
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/tickets/*/use"
                        ).hasAnyRole("STAFF", "ADMIN")

                        // Consultas de usuario: autenticación y propiedad
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/by-email",
                                "/api/users/by-username"
                        ).hasAnyRole("USER", "STAFF", "ADMIN")

                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    private void writeError(
            HttpServletResponse response,
            HttpStatus status,
            String message) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                Map.of()
        );

        objectMapper.writeValue(response.getOutputStream(), body);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }
}
