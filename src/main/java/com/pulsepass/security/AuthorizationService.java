package com.pulsepass.security;

import com.pulsepass.entity.Event;

import com.pulsepass.entity.Ticket;

import com.pulsepass.enums.EventStatus;

import com.pulsepass.repository.EventRepository;

import com.pulsepass.repository.TicketRepository;

import com.pulsepass.repository.UserRepository;

import org.springframework.security.authentication.AnonymousAuthenticationToken;

import org.springframework.security.core.Authentication;

import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

@Service("authorizationService")

public class AuthorizationService {

    private final UserRepository users;

    private final TicketRepository tickets;

    private final EventRepository events;

    public AuthorizationService(

            UserRepository users,

            TicketRepository tickets,

            EventRepository events) {

        this.users = users;

        this.tickets = tickets;

        this.events = events;

    }

    public Long currentUserId() {

        Authentication authentication =

                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null

                || !authentication.isAuthenticated()

                || authentication instanceof AnonymousAuthenticationToken) {

            return null;

        }

        try {

            return Long.valueOf(authentication.getName());

        } catch (NumberFormatException exception) {

            return null;

        }

    }

    /**

     * Comprueba el propietario mediante el ID persistente,

     * no confiando únicamente en el correo recibido.

     */

    @Transactional(readOnly = true)

    public boolean isCurrentUserEmail(String email) {

        Long currentId = currentUserId();

        if (currentId == null || email == null || email.isBlank()) {

            return false;

        }

        return users.findByEmailIgnoreCase(email.trim())

                .map(user -> currentId.equals(user.getId()))

                .orElse(false);

    }

    public boolean canAccessUserEmail(String email) {

        return hasRole("ADMIN") || isCurrentUserEmail(email);

    }

    @Transactional(readOnly = true)

    public boolean canAccessUserName(String username) {

        if (hasRole("ADMIN")) {

            return true;

        }

        Long currentId = currentUserId();

        if (currentId == null || username == null || username.isBlank()) {

            return false;

        }

        return users.findByUsername(username.trim())

                .map(user -> currentId.equals(user.getId()))

                .orElse(false);

    }

    /**

     * GET de un ticket:

     * propietario, STAFF o ADMIN.

     */

    @Transactional(readOnly = true)

    public boolean canAccessTicket(String ticketCode) {

        if (hasRole("ADMIN") || hasRole("STAFF")) {

            return true;

        }

        return isTicketOwnedByCurrentUser(ticketCode);

    }

    /**

     * GET /api/tickets/by-user:

     * propietario o ADMIN. STAFF no tiene acceso general por correo.

     */

    @Transactional(readOnly = true)

    public boolean canAccessTicketsByEmail(String email) {

        return hasRole("ADMIN") || isCurrentUserEmail(email);

    }

    /**

     * Cancelación:

     * ADMIN o USER propietario.

     */

    @Transactional(readOnly = true)

    public boolean canCancelTicket(String ticketCode) {

        if (hasRole("ADMIN")) {

            return true;

        }

        if (!hasRole("USER")) {

            return false;

        }

        return isTicketOwnedByCurrentUser(ticketCode);

    }

    @Transactional(readOnly = true)

    public boolean isTicketOwnedByCurrentUser(String ticketCode) {

        Long currentId = currentUserId();

        if (currentId == null || ticketCode == null || ticketCode.isBlank()) {

            return false;

        }

        return tickets.findByTicketCode(ticketCode.trim())

                .map(Ticket::getUser)

                .map(user -> currentId.equals(user.getId()))

                .orElse(false);

    }

    /**

     * La visibilidad pública se limita a eventos PUBLISHED.

     * STAFF y ADMIN pueden consultar otros estados mediante

     * la autorización del controlador.

     */

    @Transactional(readOnly = true)

    public boolean isPublishedEvent(String eventCode) {

        if (eventCode == null || eventCode.isBlank()) {

            return false;

        }

        return events.findByEventCode(eventCode.trim())

                .map(Event::getStatus)

                .map(status -> status == EventStatus.PUBLISHED)

                .orElse(false);

    }

    @Transactional(readOnly = true)

    public boolean hasPublishedEventsForArtist(String stageName) {

        if (stageName == null || stageName.isBlank()) {

            return false;

        }

        return !events.findByArtistStageNameAndStatus(

                stageName.trim(),

                EventStatus.PUBLISHED

        ).isEmpty();

    }

    public boolean hasRole(String role) {

        Authentication authentication =

                SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getAuthorities().stream()

                .anyMatch(authority ->

                        authority.getAuthority().equals("ROLE_" + role));

    }

}


