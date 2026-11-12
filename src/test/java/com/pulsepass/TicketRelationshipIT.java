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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;


import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class TicketRelationshipIT {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Test
    void shouldPersistTicketWithUserAndEvent() {

        User user = new User();

        user.setUsername("relationship-user");
        user.setEmail("relationship@test.com");
        user.setActive(true);

        user = userRepository.save(user);

        Venue venue = new Venue();

        venue.setCode("VEN-TICKET-REL");
        venue.setName("Ticket Relationship Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Test Address");
        venue.setCapacity(2000);
        venue.setActive(true);

        venue = venueRepository.save(venue);

        Event event = new Event();

        event.setEventCode("EVENT-TICKET-REL");
        event.setName("Ticket Relationship Event");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDate.now().plusDays(50));
        event.setMinimumAge(18);
        event.setVenue(venue);

        event = eventRepository.save(event);

        Ticket ticket = new Ticket();

        ticket.setTicketCode("TICKET-REL-01");
        ticket.setType(TicketType.VIP);
        ticket.setPrice(new BigDecimal("250000"));
        ticket.setStatus(TicketStatus.PAID);
        ticket.setPurchaseDate(LocalDate.now());
        ticket.setUser(user);
        ticket.setEvent(event);

        ticketRepository.saveAndFlush(ticket);

        Ticket result =
                ticketRepository.findById(ticket.getId())
                        .orElseThrow();

        assertNotNull(result.getUser());
        assertNotNull(result.getEvent());

        assertEquals(
                "relationship@test.com",
                result.getUser().getEmail()
        );

        assertEquals(
                "EVENT-TICKET-REL",
                result.getEvent().getEventCode()
        );
    }
}