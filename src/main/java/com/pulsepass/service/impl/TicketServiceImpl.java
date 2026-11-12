package com.pulsepass.service.impl;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.entity.Event;
import com.pulsepass.entity.Ticket;
import com.pulsepass.entity.User;
import com.pulsepass.enums.EventStatus;
import com.pulsepass.enums.TicketStatus;
import com.pulsepass.enums.TicketType;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
public class TicketServiceImpl implements TicketService {
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketMapper ticketMapper;

    public TicketServiceImpl(TicketRepository ticketRepository, UserRepository userRepository,
                             EventRepository eventRepository, TicketMapper ticketMapper) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.ticketMapper = ticketMapper;
    }

    @Override
    @Transactional
    public TicketResponse purchase(PurchaseTicketRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.userEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userEmail()));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessRuleException("User is inactive: " + request.userEmail());
        }

        Event event = eventRepository.findByEventCode(request.eventCode())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.eventCode()));
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException("Tickets can only be purchased for PUBLISHED events.");
        }
        if (event.getEventDate() == null || !event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot purchase a ticket for an event that has already occurred.");
        }
        validateAge(user, event);

        long paidTickets = ticketRepository.countByEventEventCodeAndStatus(event.getEventCode(), TicketStatus.PAID);
        if (paidTickets >= event.getVenue().getCapacity()) {
            throw new BusinessRuleException("Event is sold out.");
        }

        Ticket ticket = new Ticket();
        ticket.setTicketCode(generateTicketCode());
        ticket.setType(request.type());
        ticket.setPrice(calculatePrice(request.type()));
        ticket.setStatus(TicketStatus.PAID);
        ticket.setPurchaseDate(LocalDate.now());
        ticket.setUser(user);
        ticket.setEvent(event);
        Ticket saved = ticketRepository.save(ticket);

        if (paidTickets + 1 == event.getVenue().getCapacity()) {
            event.setStatus(EventStatus.SOLD_OUT);
            eventRepository.save(event);
        }
        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse findByCode(String ticketCode) {
        return ticketMapper.toResponse(getTicket(ticketCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findByUserEmail(String email) {
        return ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(email)
                .stream().map(ticketMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findPaidTicketsByEvent(String eventCode) {
        return ticketRepository.findByEventEventCodeAndStatus(eventCode, TicketStatus.PAID)
                .stream().map(ticketMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public TicketResponse cancel(String ticketCode) {
        Ticket ticket = getTicket(ticketCode);
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException("Only PAID tickets can be cancelled.");
        }
        if (ticket.getEvent().getEventDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Ticket cannot be cancelled after the event date.");
        }
        ticket.setStatus(TicketStatus.CANCELLED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public TicketResponse markAsUsed(String ticketCode) {
        Ticket ticket = getTicket(ticketCode);
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException("Only PAID tickets can be marked as USED.");
        }
        ticket.setStatus(TicketStatus.USED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    private void validateAge(User user, Event event) {
        if (event.getMinimumAge() == null || event.getMinimumAge() == 0) return;
        if (user.getProfile() == null || user.getProfile().getBirthDate() == null) {
            throw new BusinessRuleException("User birth date is required for this event.");
        }
        int age = Period.between(user.getProfile().getBirthDate(), event.getEventDate().toLocalDate()).getYears();
        if (age < event.getMinimumAge()) {
            throw new BusinessRuleException("User does not meet minimum age.");
        }
    }

    private BigDecimal calculatePrice(TicketType type) {
        return switch (type) {
            case GENERAL -> new BigDecimal("120000");
            case STUDENT -> new BigDecimal("84000");
            case VIP -> new BigDecimal("250000");
            case BACKSTAGE -> new BigDecimal("350000");
        };
    }

    private String generateTicketCode() {
        return "TCK-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    private Ticket getTicket(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));
    }
}
