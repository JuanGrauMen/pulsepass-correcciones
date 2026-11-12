package com.pulsepass.controller;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) { this.ticketService = ticketService; }

    @PostMapping
    @PreAuthorize("hasRole('USER') and @authorizationService.isCurrentUserEmail(#request.userEmail())")
    public ResponseEntity<TicketResponse> purchase(@Valid @RequestBody PurchaseTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.purchase(request));
    }

    @GetMapping("/{ticketCode}")
    @PreAuthorize("hasAnyRole('USER', 'STAFF', 'ADMIN') and @authorizationService.canAccessTicket(#ticketCode)")
    public ResponseEntity<TicketResponse> findByCode(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ticketService.findByCode(ticketCode));
    }

    @GetMapping("/by-user")
    @PreAuthorize("hasAnyRole('USER', 'STAFF', 'ADMIN') and @authorizationService.canAccessTicketsByEmail(#email)")
    public ResponseEntity<List<TicketResponse>> findByUserEmail(@RequestParam String email) {
        return ResponseEntity.ok(ticketService.findByUserEmail(email));
    }
    @PatchMapping("/{ticketCode}/cancel")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN') and @authorizationService.canCancelTicket(#ticketCode)")
    public ResponseEntity<TicketResponse> cancel(
            @PathVariable String ticketCode) {
        return ResponseEntity.ok(ticketService.cancel(ticketCode));
    }

    @PatchMapping("/{ticketCode}/use")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<TicketResponse> markAsUsed(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ticketService.markAsUsed(ticketCode));
    }
}
