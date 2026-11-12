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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class EventArtistIT {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Test
    void shouldPersistEventWithMultipleArtists() {

        Artist artist1 = new Artist();
        artist1.setStageName("Artist IT One");
        artist1.setCountry("Colombia");
        artist1.setGenre("Music");
        artist1.setActive(true);

        Artist artist2 = new Artist();
        artist2.setStageName("Artist IT Two");
        artist2.setCountry("Colombia");
        artist2.setGenre("Music");
        artist2.setActive(true);

        artist1 = artistRepository.save(artist1);
        artist2 = artistRepository.save(artist2);

        Venue venue = new Venue();
        venue.setCode("VEN-ARTIST-01");
        venue.setName("Artist Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Test Address");
        venue.setCapacity(3000);
        venue.setActive(true);

        venue = venueRepository.save(venue);

        Event event = new Event();

        event.setEventCode("EVENT-ARTIST-01");
        event.setName("Event With Artists");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDate.now().plusDays(30));
        event.setMinimumAge(18);
        event.setVenue(venue);

        event.getArtists().add(artist1);
        event.getArtists().add(artist2);

        eventRepository.saveAndFlush(event);

        Event result =
                eventRepository.findByEventCode("EVENT-ARTIST-01")
                        .orElseThrow();

        assertEquals(2, result.getArtists().size());

        assertTrue(
                result.getArtists()
                        .stream()
                        .anyMatch(a -> a.getStageName().equals("Artist IT One"))
        );

        assertTrue(
                result.getArtists()
                        .stream()
                        .anyMatch(a -> a.getStageName().equals("Artist IT Two"))
        );
    }

    @Test
    void shouldFindEventsByArtistWithoutDuplicates() {

        Artist artist = new Artist();

        artist.setStageName("Unique Search Artist");
        artist.setCountry("Colombia");
        artist.setGenre("Rock");
        artist.setActive(true);

        artist = artistRepository.save(artist);

        Venue venue = new Venue();

        venue.setCode("VEN-ARTIST-02");
        venue.setName("Search Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Test Address");
        venue.setCapacity(2000);
        venue.setActive(true);

        venue = venueRepository.save(venue);

        Event event = new Event();

        event.setEventCode("EVENT-ARTIST-02");
        event.setName("Search Artist Event");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDate.now().plusDays(45));
        event.setMinimumAge(18);
        event.setVenue(venue);
        event.getArtists().add(artist);

        eventRepository.saveAndFlush(event);

        List<Event> results =
                eventRepository.findByArtistStageName(
                        "Unique Search Artist"
                );

        assertEquals(1, results.size());
        assertEquals(
                "EVENT-ARTIST-02",
                results.get(0).getEventCode()
        );
    }

    @Test
    void shouldRejectDuplicateArtistStageName() {
        Artist first = new Artist();
        first.setStageName("Duplicate Artist Stage Name");
        first.setCountry("Colombia");
        first.setGenre("Pop");
        first.setActive(true);
        artistRepository.saveAndFlush(first);

        Artist duplicate = new Artist();
        duplicate.setStageName("Duplicate Artist Stage Name");
        duplicate.setCountry("Colombia");
        duplicate.setGenre("Rock");
        duplicate.setActive(true);

        assertThrows(DataIntegrityViolationException.class,
                () -> artistRepository.saveAndFlush(duplicate));
    }

}