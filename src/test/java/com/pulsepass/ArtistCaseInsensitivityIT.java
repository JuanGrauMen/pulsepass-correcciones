package com.pulsepass;

import com.pulsepass.entity.Artist;
import com.pulsepass.entity.Event;
import com.pulsepass.entity.Venue;
import com.pulsepass.enums.EventCategory;
import com.pulsepass.enums.EventStatus;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El nombre artistico se trata sin distinguir mayusculas en todas partes:
 * en la busqueda del artista, en la busqueda de eventos por artista y en la
 * restriccion de unicidad de la base de datos.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ArtistCaseInsensitivityIT {

    @Autowired ArtistRepository artistRepository;
    @Autowired EventRepository eventRepository;
    @Autowired VenueRepository venueRepository;

    @BeforeEach
    void seed() {
        Venue venue = new Venue();
        venue.setCode("CASE-VEN");
        venue.setName("Case Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Calle 1");
        venue.setCapacity(100);
        venue.setActive(true);
        venueRepository.saveAndFlush(venue);

        Artist solar = artistRepository.findByStageName("Solar Beat").orElseThrow();

        Event event = new Event();
        event.setEventCode("CASE-EV");
        event.setName("Evento de mayusculas");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setMinimumAge(0);
        event.setVenue(venue);
        event.getArtists().add(solar);
        eventRepository.saveAndFlush(event);
    }

    @Test
    void artistLookupByStageName_ignoresCase() {
        assertThat(artistRepository.findByStageNameIgnoreCase("solar beat")).isPresent();
        assertThat(artistRepository.findByStageNameIgnoreCase("SOLAR BEAT")).isPresent();
    }

    @Test
    void eventsByArtist_ignoreCaseOfTheStageName() {
        assertThat(eventRepository.findByArtistStageName("Solar Beat")).hasSize(1);
        assertThat(eventRepository.findByArtistStageName("solar beat")).hasSize(1);
        assertThat(eventRepository.findByArtistStageName("SOLAR BEAT")).hasSize(1);
    }

    @Test
    void publishedEventsByArtist_ignoreCaseOfTheStageName() {
        assertThat(eventRepository
                .findByArtistStageNameAndStatus("solar beat", EventStatus.PUBLISHED)).hasSize(1);
    }

    @Test
    void eventsByCityAndArtist_ignoreCaseOfTheStageName() {
        assertThat(eventRepository
                .findByCityAndArtistStageName("Santa Marta", "solar beat")).hasSize(1);
    }

    @Test
    void database_rejectsAStageNameThatDiffersOnlyInCase() {
        Artist duplicate = new Artist();
        duplicate.setStageName("SOLAR BEAT");
        duplicate.setCountry("Colombia");
        duplicate.setGenre("Pop");
        duplicate.setActive(true);

        assertThatThrownBy(() -> artistRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
