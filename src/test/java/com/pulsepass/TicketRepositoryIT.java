package com.pulsepass;

import com.pulsepass.entity.Event;
import com.pulsepass.entity.Ticket;
import com.pulsepass.entity.User;
import com.pulsepass.entity.Venue;
import com.pulsepass.enums.EventCategory;
import com.pulsepass.enums.EventStatus;
import com.pulsepass.enums.TicketStatus;
import com.pulsepass.enums.TicketType;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class TicketRepositoryIT {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Test
    void shouldFindPaidTicketsByEventCode() {

        User user = createUser("ticket-user-1", "ticket1@test.com");
        Venue venue = createVenue("VEN-TICKET-01");

        Event event = createEvent(
                "TICKET-EVENT-01",
                venue,
                LocalDate.now().plusDays(30)
        );

        Ticket paidTicket = createTicket(
                "TICKET-PAID-01",
                user,
                event,
                TicketStatus.PAID
        );

        Ticket reservedTicket = createTicket(
                "TICKET-RESERVED-01",
                user,
                event,
                TicketStatus.RESERVED
        );

        ticketRepository.save(paidTicket);
        ticketRepository.save(reservedTicket);

        List<Ticket> results =
                ticketRepository.findByEventEventCodeAndStatus(
                        "TICKET-EVENT-01",
                        TicketStatus.PAID
                );

        assertEquals(1, results.size());
        assertEquals("TICKET-PAID-01", results.get(0).getTicketCode());
    }

    @Test
    void shouldCountPaidTicketsByEventCode() {

        User user = createUser("ticket-user-2", "ticket2@test.com");
        Venue venue = createVenue("VEN-TICKET-02");

        Event event = createEvent(
                "TICKET-EVENT-02",
                venue,
                LocalDate.now().plusDays(40)
        );

        ticketRepository.save(
                createTicket(
                        "TICKET-PAID-02",
                        user,
                        event,
                        TicketStatus.PAID
                )
        );

        ticketRepository.save(
                createTicket(
                        "TICKET-PAID-03",
                        user,
                        event,
                        TicketStatus.PAID
                )
        );

        ticketRepository.save(
                createTicket(
                        "TICKET-CANCELLED-01",
                        user,
                        event,
                        TicketStatus.CANCELLED
                )
        );

        long count =
                ticketRepository.countByEventCodeAndStatus(
                        "TICKET-EVENT-02",
                        TicketStatus.PAID
                );

        assertEquals(2, count);
    }

    @Test
    void shouldFindTicketsForEventsAfterDateOrderedChronologically() {

        User user = createUser("ticket-user-3", "ticket3@test.com");

        Venue venue = createVenue("VEN-TICKET-03");

        Event laterEvent = createEvent(
                "TICKET-EVENT-LATER",
                venue,
                LocalDate.now().plusDays(60)
        );

        Event earlierEvent = createEvent(
                "TICKET-EVENT-EARLIER",
                venue,
                LocalDate.now().plusDays(20)
        );

        ticketRepository.save(
                createTicket(
                        "TICKET-LATER",
                        user,
                        laterEvent,
                        TicketStatus.PAID
                )
        );

        ticketRepository.save(
                createTicket(
                        "TICKET-EARLIER",
                        user,
                        earlierEvent,
                        TicketStatus.PAID
                )
        );

        List<Ticket> results =
                ticketRepository.findByEventEventDateAfterOrderByEventEventDateAsc(
                        LocalDate.now()
                );

        assertEquals(2, results.size());

        assertEquals(
                "TICKET-EARLIER",
                results.get(0).getTicketCode()
        );

        assertEquals(
                "TICKET-LATER",
                results.get(1).getTicketCode()
        );
    }

    private User createUser(String username, String email) {

        User user = new User();

        user.setUsername(username);
        user.setEmail(email);
        user.setActive(true);

        return userRepository.save(user);
    }

    private Venue createVenue(String code) {

        Venue venue = new Venue();

        venue.setCode(code);
        venue.setName("Test Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Test Address");
        venue.setCapacity(1000);
        venue.setActive(true);

        return venueRepository.save(venue);
    }

    private Event createEvent(
            String eventCode,
            Venue venue,
            LocalDate eventDate
    ) {

        Event event = new Event();

        event.setEventCode(eventCode);
        event.setName("Test Event");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(eventDate);
        event.setMinimumAge(18);
        event.setVenue(venue);

        return eventRepository.save(event);
    }

    private Ticket createTicket(
            String ticketCode,
            User user,
            Event event,
            TicketStatus status
    ) {

        Ticket ticket = new Ticket();

        ticket.setTicketCode(ticketCode);
        ticket.setType(TicketType.GENERAL);
        ticket.setPrice(new BigDecimal("120000"));
        ticket.setStatus(status);
        ticket.setPurchaseDate(LocalDate.now());
        ticket.setUser(user);
        ticket.setEvent(event);

        return ticket;
    }

    @Test
    void shouldRejectDuplicateTicketCode() {
        User user = createUser("ticket-duplicate-user", "ticket-duplicate@test.com");
        Venue venue = createVenue("VEN-TICKET-DUPLICATE");
        Event event = createEvent(
                "TICKET-EVENT-DUPLICATE", venue, LocalDate.now().plusDays(25));

        ticketRepository.saveAndFlush(createTicket(
                "TICKET-CODE-DUPLICATE", user, event, TicketStatus.PAID));

        assertThrows(DataIntegrityViolationException.class,
                () -> ticketRepository.saveAndFlush(createTicket(
                        "TICKET-CODE-DUPLICATE", user, event, TicketStatus.PAID)));
    }

    @Test
    void shouldFindTicketsByUserEmailIgnoringCaseAndStatus() {
        User user = createUser("ticket-query-user", "ticket-query@test.com");
        Venue venue = createVenue("VEN-TICKET-QUERY");
        Event event = createEvent(
                "TICKET-EVENT-QUERY", venue, LocalDate.now().plusDays(25));

        ticketRepository.save(createTicket(
                "TICKET-QUERY-PAID-1", user, event, TicketStatus.PAID));
        ticketRepository.save(createTicket(
                "TICKET-QUERY-PAID-2", user, event, TicketStatus.PAID));
        ticketRepository.save(createTicket(
                "TICKET-QUERY-RESERVED", user, event, TicketStatus.RESERVED));
        ticketRepository.flush();

        List<Ticket> results = ticketRepository.findByUserEmailIgnoreCaseAndStatus(
                "TICKET-QUERY@TEST.COM", TicketStatus.PAID);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(ticket -> ticket.getStatus() == TicketStatus.PAID));
    }


    @Test
    void shouldRejectNegativeTicketPrice() {
        User user = createUser("ticket-negative-price-user", "ticket-negative-price@test.com");
        Venue venue = createVenue("VEN-TICKET-NEG-PRICE");
        Event event = createEvent(
                "TICKET-EVENT-NEG-PRICE", venue, LocalDate.now().plusDays(25));

        Ticket invalid = createTicket(
                "TICKET-NEGATIVE-PRICE", user, event, TicketStatus.PAID);
        invalid.setPrice(new BigDecimal("-1.00"));

        assertThrows(DataIntegrityViolationException.class,
                () -> ticketRepository.saveAndFlush(invalid));
    }

}