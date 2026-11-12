package com.pulsepass.dto.request;

import com.pulsepass.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterUserRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone,
        String city,
        @NotNull LocalDate birthDate,
        @NotBlank
        @Size(min = 12, message = "Password must contain at least 12 characters")
        @MaxUtf8Bytes(value = 72, message = "Password must not exceed 72 bytes when encoded as UTF-8")
        String password
) {}