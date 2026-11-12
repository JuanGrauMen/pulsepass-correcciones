package com.pulsepass.controller;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtService;
import com.pulsepass.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser(roles = "USER")
class UserControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    UserService userService;

    private static final String VALID_USER = """
            {
              "username":"andrea",
              "email":"andrea@email.com",
              "firstName":"Andrea",
              "lastName":"Diaz",
              "phone":"3000000000",
              "city":"Santa Marta",
              "birthDate":"2000-01-01",
              "password":"PulsePassTest!2026"
            }
            """;

    private UserResponse userResponse() {
        return new UserResponse(
                1L,
                "andrea",
                "andrea@email.com",
                true,
                "Andrea",
                "Diaz",
                "3000000000",
                "Santa Marta",
                LocalDate.of(2000, 1, 1)
        );
    }

    @Test
    void register_valid_returns201() throws Exception {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenReturn(userResponse());

        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@email.com"));

        verify(userService).register(any(RegisterUserRequest.class));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        String invalidUser = VALID_USER.replace(
                "andrea@email.com", "invalid-email");

        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidUser))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400));

        verify(userService, never())
                .register(any(RegisterUserRequest.class));
    }

    @Test
    void register_passwordShorterThan12_returns400() throws Exception {
        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER.replace("PulsePassTest!2026", "Short1!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.password").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void register_passwordOver72Bytes_returns400NotConflict() throws Exception {
        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER.replace("PulsePassTest!2026", "a".repeat(73))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.password").exists());

        verifyNoInteractions(userService);
    }

    @Test
    void register_invalidPasswordWithExistingUsername_returns400BeforeCheckingDuplicates() throws Exception {
        // Aunque el servicio respondiera "duplicado", la contrasena invalida
        // debe rechazarse antes de llegar a el: nunca se revela la existencia.
        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username already exists"));

        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER.replace("PulsePassTest!2026", "a".repeat(73))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username already exists"));

        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void findByEmail_returns200() throws Exception {
        when(userService.findByEmail("andrea@email.com")).thenReturn(userResponse());

        mvc.perform(get("/api/users/by-email")
                        .param("email", "andrea@email.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("andrea@email.com"));

        verify(userService).findByEmail("andrea@email.com");
    }

    @Test
    void findByUsername_returns200() throws Exception {
        when(userService.findByUsername("andrea")).thenReturn(userResponse());

        mvc.perform(get("/api/users/by-username")
                        .param("username", "andrea"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"));

        verify(userService).findByUsername("andrea");
    }

    @Test
    void findByEmail_missing_returns404() throws Exception {
        when(userService.findByEmail("missing@email.com"))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mvc.perform(get("/api/users/by-email")
                        .param("email", "missing@email.com"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void findByUsername_missing_returns404() throws Exception {
        when(userService.findByUsername("missing"))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mvc.perform(get("/api/users/by-username")
                        .param("username", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Email already exists"));

        mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void findByEmail_missingRequiredParameter_returns400() throws Exception {
        mvc.perform(get("/api/users/by-email"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));

        verifyNoInteractions(userService);
    }

}
