package com.pulsepass.controller;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.enums.TicketStatus;
import com.pulsepass.enums.TicketType;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtService;
import com.pulsepass.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser(roles = "USER")
class TicketControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    TicketService ticketService;

    private static final String VALID_TICKET = """
            {
              "userEmail":"andrea@email.com",
              "eventCode":"CMF-2027",
              "type":"GENERAL"
            }
            """;

    private TicketResponse ticketResponse(TicketStatus status) {
        return new TicketResponse(
                1L,
                "TCK-001",
                TicketType.GENERAL,
                new BigDecimal("120000.00"),
                status,
                LocalDate.of(2026, 10, 17),
                "andrea@email.com",
                "CMF-2027",
                "Caribbean Music Fest"
        );
    }

    @Test
    void purchase_valid_returns201() throws Exception {
        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenReturn(ticketResponse(TicketStatus.PAID));

        mvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TICKET))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TCK-001"))
                .andExpect(jsonPath("$.status").value("PAID"));

        verify(ticketService).purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    void purchase_invalidEmail_returns400() throws Exception {
        String invalid = VALID_TICKET.replace(
                "andrea@email.com", "invalid-email");

        mvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400));

        verify(ticketService, never())
                .purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    void findByCode_existing_returns200() throws Exception {
        when(ticketService.findByCode("TCK-001"))
                .thenReturn(ticketResponse(TicketStatus.PAID));

        mvc.perform(get("/api/tickets/TCK-001"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TCK-001"))
                .andExpect(jsonPath("$.status").value("PAID"));

        verify(ticketService).findByCode("TCK-001");
    }

    @Test
    void findByCode_missing_returns404() throws Exception {
        when(ticketService.findByCode("MISSING"))
                .thenThrow(new ResourceNotFoundException("Ticket not found: MISSING"));

        mvc.perform(get("/api/tickets/MISSING"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Ticket not found: MISSING"));
    }

    @Test
    void findByUserEmail_returns200() throws Exception {
        when(ticketService.findByUserEmail("andrea@email.com"))
                .thenReturn(List.of(ticketResponse(TicketStatus.PAID)));

        mvc.perform(get("/api/tickets/by-user")
                        .param("email", "andrea@email.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].ticketCode").value("TCK-001"));

        verify(ticketService).findByUserEmail("andrea@email.com");
    }

    @Test
    void cancel_paidTicket_returns200() throws Exception {
        when(ticketService.cancel("TCK-001"))
                .thenReturn(ticketResponse(TicketStatus.CANCELLED));

        mvc.perform(patch("/api/tickets/TCK-001/cancel"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(ticketService).cancel("TCK-001");
    }

    @Test
    void cancel_invalidTransition_returns409() throws Exception {
        when(ticketService.cancel("TCK-USED"))
                .thenThrow(new BusinessRuleException("Used tickets cannot be cancelled"));

        mvc.perform(patch("/api/tickets/TCK-USED/cancel"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void markAsUsed_paidTicket_returns200() throws Exception {
        when(ticketService.markAsUsed("TCK-001"))
                .thenReturn(ticketResponse(TicketStatus.USED));

        mvc.perform(patch("/api/tickets/TCK-001/use"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService).markAsUsed("TCK-001");
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void markAsUsed_cancelledTicket_returns409() throws Exception {
        when(ticketService.markAsUsed("TCK-CANCELLED"))
                .thenThrow(new BusinessRuleException("Cancelled tickets cannot be used"));

        mvc.perform(patch("/api/tickets/TCK-CANCELLED/use"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void findByUserEmail_missingRequiredParameter_returns400() throws Exception {
        mvc.perform(get("/api/tickets/by-user"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request parameters"));

        verifyNoInteractions(ticketService);
    }

}
