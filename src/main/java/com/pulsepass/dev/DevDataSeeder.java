package com.pulsepass.dev;

import com.pulsepass.entity.Artist;
import com.pulsepass.entity.Event;
import com.pulsepass.entity.User;
import com.pulsepass.entity.UserCredential;
import com.pulsepass.entity.UserProfile;
import com.pulsepass.entity.UserRole;
import com.pulsepass.entity.Venue;
import com.pulsepass.enums.EventCategory;
import com.pulsepass.enums.EventStatus;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserProfileRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.repository.VenueRepository;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Datos de desarrollo y de pruebas E2E manuales. Solo existe con el perfil
 * {@code dev}; no es una migracion de Flyway, por lo que nunca se aplica en
 * produccion.
 *
 * Crea lo que no se puede crear por HTTP: las cuentas STAFF y ADMIN (el
 * registro publico solo produce USER) y una cuenta inactiva, mas las sedes y
 * los eventos de los escenarios de prueba. USER_A y USER_B se registran por la
 * API durante las pruebas.
 *
 * La contrasena de las tres cuentas viene de PULSEPASS_DEV_SEED_PASSWORD y no
 * tiene valor por defecto: sin ella la semilla se niega a ejecutarse, de modo
 * que ninguna cuenta privilegiada queda con una clave conocida. En la base se
 * guarda solo el hash BCrypt.
 *
 * Es idempotente: crea unicamente lo que falta. Las fechas se calculan desde el
 * momento de la primera ejecucion y quedan a mas de 180 dias; si la base de
 * datos se conserva durante mucho tiempo, hay que recrearla.
 */
@Component
@Profile("dev")
public class DevDataSeeder implements SmartInitializingSingleton {

    static final String PASSWORD_VARIABLE = "PULSEPASS_DEV_SEED_PASSWORD";

    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final ArtistRepository artistRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedPassword;

    public DevDataSeeder(
            VenueRepository venueRepository,
            EventRepository eventRepository,
            ArtistRepository artistRepository,
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserCredentialRepository userCredentialRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${pulsepass.dev.seed-password:}") String seedPassword) {
        this.venueRepository = venueRepository;
        this.eventRepository = eventRepository;
        this.artistRepository = artistRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedPassword = seedPassword;
    }

    /**
     * Se ejecuta cuando todos los beans estan creados y antes de que el
     * servidor web empiece a aceptar peticiones (a diferencia de un
     * ApplicationRunner, que corre cuando la API ya esta atendiendo). Asi no
     * hay un instante en que la API responda sin las cuentas sembradas.
     * Si la semilla falla, la aplicacion no arranca.
     */
    @Override
    @Transactional
    public void afterSingletonsInstantiated() {
        seed();
    }

    @Transactional
    public void seed() {
        requireValidPassword();

        Venue marina = venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta",
                "Carrera 1 # 10-20", 5000, true);
        Venue tiny = venue("VEN-SMR-02", "Sala Intima Santa Marta", "Santa Marta",
                "Calle 15 # 3-40", 3, true);
        venue("VEN-OFF-01", "Auditorio Cerrado", "Barranquilla",
                "Avenida 30 # 45-10", 200, false);

        Artist caribbean = artistRepository.findByStageName("Caribbean Sound").orElse(null);
        Artist solar = artistRepository.findByStageName("Solar Beat").orElse(null);

        event("CMF-2026", "Caribbean Music Fest 2026", EventCategory.MUSIC,
                EventStatus.PUBLISHED, 180, 0, marina, caribbean);
        event("EV-DRAFT-01", "Evento en borrador", EventCategory.CULTURE,
                EventStatus.DRAFT, 200, 0, marina, solar);
        event("EV-TOPUBLISH-01", "Evento por publicar", EventCategory.TECHNOLOGY,
                EventStatus.DRAFT, 210, 0, marina, null);
        event("EV-CANCELLED-01", "Evento cancelado", EventCategory.SPORTS,
                EventStatus.CANCELLED, 190, 0, marina, null);
        event("EV-ADULT-01", "Evento para mayores de edad", EventCategory.ENTERTAINMENT,
                EventStatus.PUBLISHED, 185, 18, marina, solar);
        event("EV-TINY-01", "Concierto de sala pequena", EventCategory.MUSIC,
                EventStatus.PUBLISHED, 195, 0, tiny, caribbean);

        user("admin_dev", "admin@example.test", "Admin", "Dev", true, "ADMIN");
        user("staff_dev", "staff@example.test", "Staff", "Dev", true, "STAFF");
        user("inactive_dev", "inactive@example.test", "Inactivo", "Dev", false, "USER");
    }

    private void requireValidPassword() {
        if (seedPassword == null || seedPassword.isBlank()) {
            throw new IllegalStateException(
                    "La semilla de desarrollo necesita la variable de entorno "
                            + PASSWORD_VARIABLE + " (minimo 12 caracteres y maximo 72 bytes). "
                            + "No tiene valor por defecto a proposito.");
        }
        if (seedPassword.length() < 12) {
            throw new IllegalStateException(
                    PASSWORD_VARIABLE + " debe tener al menos 12 caracteres.");
        }
        if (seedPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    PASSWORD_VARIABLE + " no puede superar 72 bytes en UTF-8.");
        }
    }

    private Venue venue(String code, String name, String city, String address,
                        int capacity, boolean active) {
        return venueRepository.findByCode(code).orElseGet(() -> {
            Venue venue = new Venue();
            venue.setCode(code);
            venue.setName(name);
            venue.setCity(city);
            venue.setAddress(address);
            venue.setCapacity(capacity);
            venue.setActive(active);
            return venueRepository.save(venue);
        });
    }

    private void event(String code, String name, EventCategory category, EventStatus status,
                       int daysAhead, int minimumAge, Venue venue, Artist artist) {
        if (eventRepository.existsByEventCode(code)) {
            return;
        }
        Event event = new Event();
        event.setEventCode(code);
        event.setName(name);
        event.setDescription("Dato de prueba para el entorno de desarrollo.");
        event.setCategory(category);
        event.setStatus(status);
        event.setEventDate(LocalDateTime.of(LocalDate.now().plusDays(daysAhead), LocalTime.of(20, 0)));
        event.setMinimumAge(minimumAge);
        event.setVenue(venue);
        if (artist != null) {
            event.getArtists().add(artist);
        }
        eventRepository.save(event);
    }

    private void user(String username, String email, String firstName, String lastName,
                      boolean active, String role) {
        if (userRepository.existsByUsername(username)
                || userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setActive(active);

        UserProfile profile = new UserProfile();
        profile.setFirstName(firstName);
        profile.setLastName(lastName);
        profile.setBirthDate(LocalDate.of(1990, 1, 1));
        profile.setUser(user);
        user.setProfile(profile);

        User saved = userRepository.save(user);
        profile.setUser(saved);
        userProfileRepository.save(profile);
        userCredentialRepository.save(new UserCredential(saved, passwordEncoder.encode(seedPassword)));
        userRoleRepository.save(new UserRole(saved.getId(), role));
    }
}
