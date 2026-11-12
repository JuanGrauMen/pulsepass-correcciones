package com.pulsepass.controller;

import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtService;
import com.pulsepass.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VenueController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser(roles = "USER")
class VenueControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    VenueService venueService;

    @Test
    void findByCode_existing_returns200() throws Exception {
        when(venueService.findByCode("VEN-SMR-01"))
                .thenReturn(new VenueResponse(
                        1L, "VEN-SMR-01", "Marina Convention Center",
                        "Santa Marta", "Carrera 1", 300, true));

        mvc.perform(get("/api/venues/VEN-SMR-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VEN-SMR-01"));

        verify(venueService).findByCode("VEN-SMR-01");
    }

    @Test
    void findByCode_missing_returns404() throws Exception {
        when(venueService.findByCode("UNKNOWN"))
                .thenThrow(new ResourceNotFoundException("Venue not found: UNKNOWN"));

        mvc.perform(get("/api/venues/UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Venue not found: UNKNOWN"));
    }

    @Test
    void findActiveVenues_returns200() throws Exception {
        when(venueService.findActiveVenues()).thenReturn(List.of());

        mvc.perform(get("/api/venues/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());

        verify(venueService).findActiveVenues();
    }
}
