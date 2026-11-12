package com.pulsepass;

import com.pulsepass.entity.Artist;
import com.pulsepass.entity.Event;
import com.pulsepass.entity.Venue;
import com.pulsepass.enums.EventCategory;
import com.pulsepass.enums.EventStatus;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class EventSearchIT {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Test
    void shouldFindEventsByArtistStageNameWithoutDuplicates() {

        Artist artist = new Artist();
        artist.setStageName("Solar Beat Prueba");
        artist.setCountry("Colombia");
        artist.setGenre("Electronic");
        artist.setActive(true);

        artist = artistRepository.save(artist);

        Venue venue = new Venue();
        venue.setCode("VEN-TEST-01");
        venue.setName("Test Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Test Address");
        venue.setCapacity(1000);
        venue.setActive(true);

        venue = venueRepository.save(venue);

        Event event = new Event();
        event.setEventCode("TEST-ARTIST-01");
        event.setName("Test Artist Event");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDate.now().plusDays(30));
        event.setMinimumAge(18);
        event.setVenue(venue);
        event.getArtists().add(artist);

        eventRepository.save(event);

        List<Event> results =
                eventRepository.findByArtistStageName("Solar Beat Prueba");

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals("TEST-ARTIST-01", results.get(0).getEventCode());
    }

    @Test
    void shouldFindEventsByCityAndArtist() {

        Artist artist = new Artist();
        artist.setStageName("Neon Waves Prueba");
        artist.setCountry("Colombia");
        artist.setGenre("Pop");
        artist.setActive(true);

        artist = artistRepository.save(artist);

        Venue venue = new Venue();
        venue.setCode("VEN-TEST-02");
        venue.setName("Santa Marta Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Test Address");
        venue.setCapacity(2000);
        venue.setActive(true);

        venue = venueRepository.save(venue);

        Event event = new Event();
        event.setEventCode("TEST-CITY-01");
        event.setName("Santa Marta Music Event");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDate.now().plusDays(40));
        event.setMinimumAge(18);
        event.setVenue(venue);
        event.getArtists().add(artist);

        eventRepository.save(event);

        List<Event> results =
                eventRepository.findByCityAndArtistStageName(
                        "Santa Marta",
                        "Neon Waves Prueba"
                );

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals("TEST-CITY-01", results.get(0).getEventCode());
    }

    @Test
    void shouldFindRecommendedEventsCaseInsensitiveAndOrdered() {

        Artist artist = new Artist();
        artist.setStageName("Caribbean Sound Prueba");
        artist.setCountry("Colombia");
        artist.setGenre("Music");
        artist.setActive(true);

        artist = artistRepository.save(artist);

        Venue venue = new Venue();
        venue.setCode("VEN-TEST-03");
        venue.setName("Recommended Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Test Address");
        venue.setCapacity(3000);
        venue.setActive(true);

        venue = venueRepository.save(venue);

        Event firstEvent = new Event();
        firstEvent.setEventCode("TEST-REC-01");
        firstEvent.setName("Recommended Event 1");
        firstEvent.setCategory(EventCategory.MUSIC);
        firstEvent.setStatus(EventStatus.PUBLISHED);
        firstEvent.setEventDate(LocalDate.now().plusDays(20));
        firstEvent.setMinimumAge(18);
        firstEvent.setVenue(venue);
        firstEvent.getArtists().add(artist);

        Event secondEvent = new Event();
        secondEvent.setEventCode("TEST-REC-02");
        secondEvent.setName("Recommended Event 2");
        secondEvent.setCategory(EventCategory.MUSIC);
        secondEvent.setStatus(EventStatus.PUBLISHED);
        secondEvent.setEventDate(LocalDate.now().plusDays(40));
        secondEvent.setMinimumAge(18);
        secondEvent.setVenue(venue);
        secondEvent.getArtists().add(artist);

        eventRepository.save(firstEvent);
        eventRepository.save(secondEvent);

        List<Event> results =
                eventRepository.findRecommendedEvents(
                        EventStatus.PUBLISHED,
                        LocalDate.now(),
                        "Santa Marta",
                        "caribbean"
                );

        assertEquals(2, results.size());
        assertEquals("TEST-REC-01", results.get(0).getEventCode());
        assertEquals("TEST-REC-02", results.get(1).getEventCode());
    }
}