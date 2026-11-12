package com.pulsepass;

import com.pulsepass.entity.Artist;
import com.pulsepass.entity.Event;
import com.pulsepass.entity.Ticket;
import com.pulsepass.entity.User;
import com.pulsepass.entity.Venue;
import com.pulsepass.enums.EventCategory;
import com.pulsepass.enums.EventStatus;
import com.pulsepass.enums.TicketStatus;
import com.pulsepass.enums.TicketType;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consultas simples de los repositories que no cubren las demas clases IT:
 * busquedas por codigo, existencia, listados de activos y ordenamientos.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class RepositoryQueriesIT {

    @Autowired VenueRepository venueRepository;
    @Autowired ArtistRepository artistRepository;
    @Autowired EventRepository eventRepository;
    @Autowired UserRepository userRepository;
    @Autowired TicketRepository ticketRepository;

    private User user;
    private Event event;

    @BeforeEach
    void seed() {
        Venue zeta = venue("Q-ZETA", "Zeta Hall", true);
        Venue alfa = venue("Q-ALFA", "Alfa Hall", true);
        venue("Q-OFF", "Cerrada", false);

        event = new Event();
        event.setEventCode("Q-EV");
        event.setName("Evento de consultas");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setMinimumAge(0);
        event.setVenue(zeta);
        eventRepository.saveAndFlush(event);

        user = new User();
        user.setUsername("Query_User");
        user.setEmail("Query.User@Example.com");
        user.setActive(true);
        userRepository.saveAndFlush(user);

        ticket("Q-T1", TicketStatus.PAID, LocalDate.now().minusDays(3));
        ticket("Q-T2", TicketStatus.PAID, LocalDate.now().minusDays(1));
        ticket("Q-T3", TicketStatus.CANCELLED, LocalDate.now().minusDays(2));
        assertThat(alfa.getId()).isNotNull();
    }

    // ---------- Venue ----------

    @Test
    void venues_activeOnes_areListedByName() {
        assertThat(venueRepository.findByActiveTrueOrderByNameAsc())
                .extracting(Venue::getName)
                .containsExactly("Alfa Hall", "Zeta Hall");
    }

    // ---------- Artist ----------

    @Test
    void artists_activeOnes_areListedByStageNameAndExcludeInactive() {
        Artist inactive = new Artist();
        inactive.setStageName("Zzz Inactive");
        inactive.setCountry("Colombia");
        inactive.setGenre("Rock");
        inactive.setActive(false);
        artistRepository.saveAndFlush(inactive);

        List<String> names = artistRepository.findByActiveTrueOrderByStageNameAsc().stream()
                .map(Artist::getStageName).toList();

        assertThat(names).hasSize(5)
                .isSortedAccordingTo(String::compareToIgnoreCase)
                .doesNotContain("Zzz Inactive");
    }

    // ---------- Event ----------

    @Test
    void events_existsByEventCode() {
        assertThat(eventRepository.existsByEventCode("Q-EV")).isTrue();
        assertThat(eventRepository.existsByEventCode("NO-SUCH")).isFalse();
    }

    // ---------- User ----------

    @Test
    void users_emailLookupIgnoresCase() {
        assertThat(userRepository.findByEmailIgnoreCase("QUERY.USER@example.COM")).isPresent();
        assertThat(userRepository.existsByEmailIgnoreCase("query.user@example.com")).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCase("nobody@example.com")).isFalse();
    }

    @Test
    void users_usernameLookupIsExact() {
        assertThat(userRepository.findByUsername("Query_User")).isPresent();
        assertThat(userRepository.existsByUsername("Query_User")).isTrue();
        // Hoy el nombre de usuario SI distingue mayusculas (decision de producto pendiente).
        assertThat(userRepository.existsByUsername("query_user")).isFalse();
    }

    // ---------- Ticket ----------

    @Test
    void tickets_findByTicketCode() {
        assertThat(ticketRepository.findByTicketCode("Q-T1")).isPresent();
        assertThat(ticketRepository.findByTicketCode("NO-SUCH")).isEmpty();
    }

    @Test
    void tickets_ofAUser_areOrderedFromNewestToOldest() {
        assertThat(ticketRepository
                .findByUserEmailIgnoreCaseOrderByPurchaseDateDesc("query.user@EXAMPLE.com"))
                .extracting(Ticket::getTicketCode)
                .containsExactly("Q-T2", "Q-T3", "Q-T1");
    }

    @Test
    void tickets_countByEventAndStatus_bothVariantsAgree() {
        assertThat(ticketRepository.countByEventEventCodeAndStatus("Q-EV", TicketStatus.PAID)).isEqualTo(2);
        assertThat(ticketRepository.countByEventCodeAndStatus("Q-EV", TicketStatus.PAID)).isEqualTo(2);
        assertThat(ticketRepository.countByEventEventCodeAndStatus("Q-EV", TicketStatus.USED)).isZero();
    }

    // ---------- Utilidades ----------

    private Venue venue(String code, String name, boolean active) {
        Venue venue = new Venue();
        venue.setCode(code);
        venue.setName(name);
        venue.setCity("Santa Marta");
        venue.setAddress("Calle 1");
        venue.setCapacity(100);
        venue.setActive(active);
        return venueRepository.saveAndFlush(venue);
    }

    private void ticket(String code, TicketStatus status, LocalDate purchaseDate) {
        Ticket ticket = new Ticket();
        ticket.setTicketCode(code);
        ticket.setType(TicketType.GENERAL);
        ticket.setPrice(new BigDecimal("50000.00"));
        ticket.setStatus(status);
        ticket.setPurchaseDate(purchaseDate);
        ticket.setUser(user);
        ticket.setEvent(event);
        ticketRepository.saveAndFlush(ticket);
    }
}
