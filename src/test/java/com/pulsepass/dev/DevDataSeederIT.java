package com.pulsepass.dev;

import com.pulsepass.TestcontainersConfiguration;
import com.pulsepass.entity.Event;
import com.pulsepass.entity.User;
import com.pulsepass.entity.UserCredential;
import com.pulsepass.entity.UserRole;
import com.pulsepass.entity.Venue;
import com.pulsepass.enums.EventStatus;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Semilla de desarrollo y de pruebas E2E: solo existe con el perfil "dev".
 * No es una migracion de Flyway, de modo que nunca se aplica en produccion.
 */
@SpringBootTest(properties = "pulsepass.dev.seed-password=DevSeed-Only-Pass-2026")
@ActiveProfiles("dev")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DevDataSeederIT {

    private static final String PASSWORD = "DevSeed-Only-Pass-2026";

    @Autowired DevDataSeeder seeder;
    @Autowired VenueRepository venueRepository;
    @Autowired EventRepository eventRepository;
    @Autowired UserRepository userRepository;
    @Autowired UserRoleRepository roleRepository;
    @Autowired UserCredentialRepository credentialRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired MockMvc mvc;

    // ---------- Sedes ----------

    @Test
    void venues_includeALargeOneASmallOneAndAnInactiveOne() {
        assertThat(venueRepository.findByCode("VEN-SMR-01")).get()
                .satisfies(v -> {
                    assertThat(v.getName()).isEqualTo("Marina Convention Center");
                    assertThat(v.getCapacity()).isEqualTo(5000);
                    assertThat(v.getActive()).isTrue();
                });
        assertThat(venueRepository.findByCode("VEN-SMR-02")).get()
                .satisfies(v -> {
                    assertThat(v.getCapacity()).isEqualTo(3);
                    assertThat(v.getActive()).isTrue();
                });
        assertThat(venueRepository.findByCode("VEN-OFF-01")).get()
                .satisfies(v -> assertThat(v.getActive()).isFalse());
    }

    // ---------- Eventos ----------

    @Test
    void events_coverEveryStatusTheE2ePlanNeeds() {
        assertThat(statusOf("CMF-2026")).isEqualTo(EventStatus.PUBLISHED);
        assertThat(statusOf("EV-DRAFT-01")).isEqualTo(EventStatus.DRAFT);
        assertThat(statusOf("EV-TOPUBLISH-01")).isEqualTo(EventStatus.DRAFT);
        assertThat(statusOf("EV-CANCELLED-01")).isEqualTo(EventStatus.CANCELLED);
        assertThat(statusOf("EV-ADULT-01")).isEqualTo(EventStatus.PUBLISHED);
        assertThat(statusOf("EV-TINY-01")).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void events_areFarEnoughInTheFutureNotToExpireSoon() {
        List<Event> seeded = eventRepository.findAll().stream()
                .filter(e -> List.of("CMF-2026", "EV-DRAFT-01", "EV-TOPUBLISH-01",
                        "EV-CANCELLED-01", "EV-ADULT-01", "EV-TINY-01").contains(e.getEventCode()))
                .toList();

        assertThat(seeded).hasSize(6);
        assertThat(seeded).allSatisfy(e ->
                assertThat(e.getEventDate()).isAfter(LocalDateTime.now().plusDays(90)));
    }

    @Test
    void events_eventForTheSmallVenueAndTheAdultEventAreConfiguredForTheirScenarios() {
        Event tiny = eventRepository.findByEventCode("EV-TINY-01").orElseThrow();
        Venue tinyVenue = tiny.getVenue();
        assertThat(tinyVenue.getCapacity()).isEqualTo(3);

        assertThat(eventRepository.findByEventCode("EV-ADULT-01").orElseThrow().getMinimumAge())
                .isEqualTo(18);
        assertThat(eventRepository.findByArtistStageName("Caribbean Sound"))
                .extracting(Event::getEventCode)
                .contains("CMF-2026", "EV-TINY-01");
    }

    // ---------- Usuarios ----------

    @Test
    void privilegedUsersHaveTheirRolesAndInactiveUserIsDisabled() {
        assertThat(rolesOf("admin@example.test")).containsExactly("ADMIN");
        assertThat(rolesOf("staff@example.test")).containsExactly("STAFF");

        User inactive = userRepository.findByEmailIgnoreCase("inactive@example.test").orElseThrow();
        assertThat(inactive.getActive()).isFalse();
    }

    @Test
    void passwordsAreStoredAsBcryptHashesOfTheConfiguredPassword() {
        for (String email : List.of("admin@example.test", "staff@example.test", "inactive@example.test")) {
            User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
            UserCredential credential = credentialRepository.findByUserId(user.getId()).orElseThrow();

            assertThat(credential.getPasswordHash()).startsWith("$2").isNotEqualTo(PASSWORD);
            assertThat(passwordEncoder.matches(PASSWORD, credential.getPasswordHash())).isTrue();
        }
    }

    // ---------- Acceso ----------

    @Test
    void adminAndStaffCanLogIn() throws Exception {
        login("admin@example.test", PASSWORD).andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
        login("staff_dev", PASSWORD).andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void inactiveUserCannotLogIn() throws Exception {
        login("inactive@example.test", PASSWORD).andExpect(status().isUnauthorized());
    }

    // ---------- Momento de la siembra ----------

    @Test
    void seedingHappensBeforeTheWebServerAcceptsRequests() {
        // Un ApplicationRunner corre cuando la API ya atiende peticiones: durante
        // un instante respondia 401 a las cuentas que aun no estaban creadas.
        // SmartInitializingSingleton corre antes de que el servidor arranque.
        assertThat(seeder).isInstanceOf(org.springframework.beans.factory.SmartInitializingSingleton.class);
        assertThat(seeder).isNotInstanceOf(org.springframework.boot.ApplicationRunner.class);
    }

    // ---------- Idempotencia ----------

    @Test
    void runningTheSeederAgainChangesNothing() throws Exception {
        long venues = venueRepository.count();
        long events = eventRepository.count();
        long users = userRepository.count();
        long userRoles = roleRepository.count();

        seeder.seed();

        assertThat(venueRepository.count()).isEqualTo(venues);
        assertThat(eventRepository.count()).isEqualTo(events);
        assertThat(userRepository.count()).isEqualTo(users);
        assertThat(roleRepository.count()).isEqualTo(userRoles);
    }

    // ---------- Utilidades ----------

    private EventStatus statusOf(String code) {
        return eventRepository.findByEventCode(code).orElseThrow().getStatus();
    }

    private List<String> rolesOf(String email) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        return roleRepository.findAllByUserId(user.getId()).stream().map(UserRole::getRole).toList();
    }

    private org.springframework.test.web.servlet.ResultActions login(String identifier, String password)
            throws Exception {
        return mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"" + identifier + "\",\"password\":\"" + password + "\"}"));
    }
}
