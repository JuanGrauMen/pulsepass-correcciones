package com.pulsepass.controller;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.service.EventService;
import com.pulsepass.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService eventService;
    private final TicketService ticketService;

    public EventController(EventService eventService, TicketService ticketService) {
        this.eventService = eventService; this.ticketService = ticketService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request));
    }

    @GetMapping("/{eventCode}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN') or @authorizationService.isPublishedEvent(#eventCode)")
    public ResponseEntity<EventResponse> findByCode(@PathVariable String eventCode) {
        return ResponseEntity.ok(eventService.findByCode(eventCode));
    }

    @GetMapping("/published")
    public ResponseEntity<List<EventSummaryResponse>> findPublishedEvents() {
        return ResponseEntity.ok(eventService.findPublishedEvents());
    }

    @PatchMapping("/{eventCode}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> publish(@PathVariable String eventCode) {
        return ResponseEntity.ok(eventService.publish(eventCode));
    }

    @PostMapping("/{eventCode}/artists/{artistId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> addArtist(@PathVariable String eventCode, @PathVariable Long artistId) {
        return ResponseEntity.ok(eventService.addArtist(eventCode, artistId));
    }

    @GetMapping("/by-artist")
    public ResponseEntity<List<EventSummaryResponse>> findByArtist(@RequestParam String stageName, Authentication authentication) {
        boolean privileged = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF") || a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(privileged ? eventService.findByArtist(stageName) : eventService.findPublishedByArtist(stageName));
    }

    @GetMapping("/{eventCode}/tickets/paid")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<List<TicketResponse>> findPaidTicketsByEvent(@PathVariable String eventCode) {
        return ResponseEntity.ok(ticketService.findPaidTicketsByEvent(eventCode));
    }
}
