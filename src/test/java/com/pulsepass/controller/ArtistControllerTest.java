package com.pulsepass.controller;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtService;
import com.pulsepass.service.ArtistService;
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

@WebMvcTest(ArtistController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser(roles = "USER")
class ArtistControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    ArtistService artistService;

    @Test
    void findById_existing_returns200() throws Exception {
        when(artistService.findById(1L))
                .thenReturn(new ArtistResponse(
                        1L, "Solar Beat", "Colombia", "Pop", true));

        mvc.perform(get("/api/artists/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(artistService).findById(1L);
    }

    @Test
    void findById_missing_returns404() throws Exception {
        when(artistService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Artist not found: 99"));

        mvc.perform(get("/api/artists/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Artist not found: 99"));
    }

    @Test
    void findByStageName_returns200() throws Exception {
        when(artistService.findByStageName("Solar Beat"))
                .thenReturn(new ArtistResponse(
                        1L, "Solar Beat", "Colombia", "Pop", true));

        mvc.perform(get("/api/artists/by-stage-name")
                        .param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(artistService).findByStageName("Solar Beat");
    }

    @Test
    void findActiveArtists_returns200() throws Exception {
        when(artistService.findActiveArtists()).thenReturn(List.of());

        mvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());

        verify(artistService).findActiveArtists();
    }

    @Test
    void findByStageName_missing_returns404() throws Exception {
        when(artistService.findByStageName("Unknown Artist"))
                .thenThrow(new ResourceNotFoundException("Artist not found: Unknown Artist"));

        mvc.perform(get("/api/artists/by-stage-name")
                        .param("stageName", "Unknown Artist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void findById_nonNumericId_returns400() throws Exception {
        mvc.perform(get("/api/artists/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(artistService);
    }

    @Test
    void findByStageName_missingRequiredParameter_returns400() throws Exception {
        mvc.perform(get("/api/artists/by-stage-name"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));

        verifyNoInteractions(artistService);
    }

}
