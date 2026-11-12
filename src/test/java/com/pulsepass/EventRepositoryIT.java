package com.pulsepass;

import com.pulsepass.entity.Event;
import com.pulsepass.entity.Venue;
import com.pulsepass.enums.EventCategory;
import com.pulsepass.enums.EventStatus;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class EventRepositoryIT {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Test
    void shouldFindPublishedEventsInDateOrder() {
        Venue venue = createVenue("VEN-EVENT-ORDER");

        Event laterPublished = createEvent(
                "EV-ORDER-LATER", venue, LocalDate.now().plusDays(30), EventStatus.PUBLISHED);
        Event earlierPublished = createEvent(
                "EV-ORDER-EARLIER", venue, LocalDate.now().plusDays(10), EventStatus.PUBLISHED);
        Event draft = createEvent(
                "EV-ORDER-DRAFT", venue, LocalDate.now().plusDays(1), EventStatus.DRAFT);

        eventRepository.save(laterPublished);
        eventRepository.save(earlierPublished);
        eventRepository.save(draft);
        eventRepository.flush();

        List<Event> result = eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED);

        assertEquals(2, result.size());
        assertEquals("EV-ORDER-EARLIER", result.get(0).getEventCode());
        assertEquals("EV-ORDER-LATER", result.get(1).getEventCode());
    }

    @Test
    void shouldFindEventsByVenueBusinessCode() {
        Venue requestedVenue = createVenue("VEN-QUERY-ONE");
        Venue otherVenue = createVenue("VEN-QUERY-TWO");

        Event expected = createEvent(
                "EV-VENUE-EXPECTED", requestedVenue, LocalDate.now().plusDays(12), EventStatus.PUBLISHED);
        Event excluded = createEvent(
                "EV-VENUE-EXCLUDED", otherVenue, LocalDate.now().plusDays(13), EventStatus.PUBLISHED);

        eventRepository.save(expected);
        eventRepository.save(excluded);
        eventRepository.flush();

        List<Event> result = eventRepository.findByVenueCode("VEN-QUERY-ONE");

        assertEquals(1, result.size());
        assertEquals("EV-VENUE-EXPECTED", result.get(0).getEventCode());
    }

    private Venue createVenue(String code) {
        Venue venue = new Venue();
        venue.setCode(code);
        venue.setName("Test Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Test Address");
        venue.setCapacity(100);
        venue.setActive(true);
        return venueRepository.saveAndFlush(venue);
    }

    private Event createEvent(String code, Venue venue, LocalDate date, EventStatus status) {
        Event event = new Event();
        event.setEventCode(code);
        event.setName("Test Event");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(status);
        event.setEventDate(date);
        event.setMinimumAge(0);
        event.setVenue(venue);
        return event;
    }

    @Test
    void shouldRejectDuplicateEventCode() {
        Venue venue = createVenue("VEN-EVENT-DUPLICATE");
        Event first = createEvent(
                "EV-CODE-DUPLICATE", venue, LocalDate.now().plusDays(10), EventStatus.DRAFT);
        eventRepository.saveAndFlush(first);

        Event duplicate = createEvent(
                "EV-CODE-DUPLICATE", venue, LocalDate.now().plusDays(15), EventStatus.PUBLISHED);

        assertThrows(DataIntegrityViolationException.class,
                () -> eventRepository.saveAndFlush(duplicate));
    }

    @Test
    void shouldRejectNegativeMinimumAge() {
        Venue venue = createVenue("VEN-EVENT-AGE-CHECK");
        Event invalid = createEvent(
                "EV-NEGATIVE-AGE", venue, LocalDate.now().plusDays(10), EventStatus.DRAFT);
        invalid.setMinimumAge(-1);

        assertThrows(DataIntegrityViolationException.class,
                () -> eventRepository.saveAndFlush(invalid));
    }

}
