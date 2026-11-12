package com.pulsepass.controller;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.enums.EventCategory;
import com.pulsepass.enums.EventStatus;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtService;
import com.pulsepass.service.EventService;
import com.pulsepass.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser(roles = "ADMIN")
class EventControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    EventService eventService;

    @MockitoBean
    TicketService ticketService;

    private static final String VALID_EVENT = """
            {
              "eventCode":"CMF-2027",
              "name":"Caribbean Music Fest",
              "description":"Music festival",
              "category":"MUSIC",
              "eventDate":"2027-12-20T19:00:00",
              "minimumAge":18,
              "venueCode":"VEN-SMR-01"
            }
            """;

    private EventResponse eventResponse() {
        return new EventResponse(
                1L,
                "CMF-2027",
                "Caribbean Music Fest",
                "Music festival",
                EventCategory.MUSIC,
                EventStatus.DRAFT,
                LocalDateTime.of(2027, 12, 20, 19, 0),
                18,
                "VEN-SMR-01",
                "Marina Convention Center",
                List.of(new ArtistResponse(7L, "Solar Beat", "Colombia", "Pop", true))
        );
    }

    @Test
    void create_valid_returns201() throws Exception {
        when(eventService.create(any(CreateEventRequest.class))).thenReturn(eventResponse());

        mvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_EVENT))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2027"))
                .andExpect(jsonPath("$.artists[0].stageName").value("Solar Beat"));

        verify(eventService).create(any(CreateEventRequest.class));
    }

    @Test
    void create_invalid_returns400() throws Exception {
        String invalidEvent = """
                {
                  "eventCode":"",
                  "name":"",
                  "category":null,
                  "eventDate":null,
                  "minimumAge":-1,
                  "venueCode":""
                }
                """;

        mvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEvent))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isMap());

        verify(eventService, never()).create(any(CreateEventRequest.class));
    }

    @Test
    void create_descriptionTooLong_returns400BeforeCallingService() throws Exception {
        String invalidEvent = VALID_EVENT.replace("Music festival", "x".repeat(2001));

        mvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEvent))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.description").exists());

        verify(eventService, never()).create(any(CreateEventRequest.class));
    }

    @Test
    void create_malformedJson_returns400BeforeCallingService() throws Exception {
        mvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed request"));

        verify(eventService, never()).create(any(CreateEventRequest.class));
    }

    @Test
    void findByCode_existing_returns200() throws Exception {
        when(eventService.findByCode("CMF-2027")).thenReturn(eventResponse());

        mvc.perform(get("/api/events/CMF-2027"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2027"));

        verify(eventService).findByCode("CMF-2027");
    }

    @Test
    void findByCode_missing_returns404() throws Exception {
        when(eventService.findByCode("UNKNOWN"))
                .thenThrow(new ResourceNotFoundException("Event not found: UNKNOWN"));

        mvc.perform(get("/api/events/UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Event not found: UNKNOWN"));
    }

    @Test
    void findPublishedEvents_returns200() throws Exception {
        when(eventService.findPublishedEvents()).thenReturn(List.of());

        mvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void unexpectedServiceException_returns500WithoutLeakingDetails() throws Exception {
        when(eventService.findPublishedEvents())
                .thenThrow(new IllegalStateException("internal database details"));

        mvc.perform(get("/api/events/published"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected internal error occurred"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("database"))));
    }

    @Test
    void publish_valid_returns200() throws Exception {
        when(eventService.publish("CMF-2027")).thenReturn(eventResponse());

        mvc.perform(patch("/api/events/CMF-2027/publish"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2027"));

        verify(eventService).publish("CMF-2027");
    }

    @Test
    void publish_invalid_returns409() throws Exception {
        when(eventService.publish("CANCELLED-EVENT"))
                .thenThrow(new BusinessRuleException("Event cannot be published"));

        mvc.perform(patch("/api/events/CANCELLED-EVENT/publish"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void addArtist_returns200() throws Exception {
        when(eventService.addArtist("CMF-2027", 7L)).thenReturn(eventResponse());

        mvc.perform(post("/api/events/CMF-2027/artists/7"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2027"));

        verify(eventService).addArtist("CMF-2027", 7L);
    }

    @Test
    void findByArtist_withoutPrivilegedAuthentication_returnsOnlyPublishedEvents() throws Exception {
        when(eventService.findPublishedByArtist("Solar Beat")).thenReturn(List.of());

        mvc.perform(get("/api/events/by-artist")
                        .param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());

        verify(eventService).findPublishedByArtist("Solar Beat");
        verify(eventService, never()).findByArtist(any());
    }

    @Test
    void findPaidTicketsByEvent_returns200() throws Exception {
        when(ticketService.findPaidTicketsByEvent("CMF-2027"))
                .thenReturn(List.of());

        mvc.perform(get("/api/events/CMF-2027/tickets/paid"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());

        verify(ticketService).findPaidTicketsByEvent("CMF-2027");
    }

    @Test
    void findByArtist_missingRequiredParameter_returns400() throws Exception {
        mvc.perform(get("/api/events/by-artist"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));

        verifyNoInteractions(eventService);
    }

}
