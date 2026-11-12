package com.pulsepass;

import com.pulsepass.entity.Event;
import com.pulsepass.entity.Venue;
import com.pulsepass.enums.EventCategory;
import com.pulsepass.enums.EventStatus;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La fecha del evento conserva la hora: la columna es TIMESTAMP (V7), no DATE.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class EventDateTimeIT {

    @Autowired EventRepository eventRepository;
    @Autowired VenueRepository venueRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    private Venue venue;

    @BeforeEach
    void seed() {
        venue = new Venue();
        venue.setCode("DT-VEN");
        venue.setName("Date Time Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Calle 1");
        venue.setCapacity(100);
        venue.setActive(true);
        venueRepository.saveAndFlush(venue);
    }

    @Test
    void eventDateColumn_isTimestamp() {
        String type = jdbcTemplate.queryForObject("""
                SELECT data_type FROM information_schema.columns
                WHERE table_name = 'events' AND column_name = 'event_date'
                """, String.class);

        assertThat(type).isEqualTo("timestamp without time zone");
    }

    @Test
    void eventTime_isPreservedWhenReadBackFromTheDatabase() {
        LocalDateTime start = LocalDate.now().plusDays(30).atTime(20, 30);
        eventRepository.saveAndFlush(event("DT-1", start));
        entityManager.clear();

        Event reloaded = eventRepository.findByEventCode("DT-1").orElseThrow();

        assertThat(reloaded.getEventDate()).isEqualTo(start);
        assertThat(reloaded.getEventDate().toLocalTime()).isEqualTo(LocalTime.of(20, 30));
    }

    @Test
    void eventsOnTheSameDay_areOrderedByTime() {
        LocalDate day = LocalDate.now().plusDays(30);
        eventRepository.saveAndFlush(event("DT-NIGHT", day.atTime(22, 0)));
        eventRepository.saveAndFlush(event("DT-MORNING", day.atTime(9, 0)));
        eventRepository.saveAndFlush(event("DT-AFTERNOON", day.atTime(16, 0)));

        assertThat(eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED))
                .extracting(Event::getEventCode)
                .containsExactly("DT-MORNING", "DT-AFTERNOON", "DT-NIGHT");
    }

    private Event event(String code, LocalDateTime eventDate) {
        Event event = new Event();
        event.setEventCode(code);
        event.setName("Evento " + code);
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(eventDate);
        event.setMinimumAge(0);
        event.setVenue(venue);
        return event;
    }
}
