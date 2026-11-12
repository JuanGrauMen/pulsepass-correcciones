package com.pulsepass.dto.request;

import com.pulsepass.enums.EventCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CreateEventRequest(
        @NotBlank String eventCode,
        @NotBlank String name,
        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,
        @NotNull EventCategory category,
        @NotNull LocalDateTime eventDate,
        @NotNull @Min(0) Integer minimumAge,
        @NotBlank String venueCode
) {}
