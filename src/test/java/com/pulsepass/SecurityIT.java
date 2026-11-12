package com.pulsepass;

import com.pulsepass.entity.Artist;
import com.pulsepass.entity.Event;
import com.pulsepass.entity.Ticket;
import com.pulsepass.entity.User;
import com.pulsepass.entity.UserCredential;
import com.pulsepass.entity.UserProfile;
import com.pulsepass.entity.UserRole;
import com.pulsepass.entity.Venue;
import com.pulsepass.enums.EventCategory;
import com.pulsepass.enums.EventStatus;
import com.pulsepass.enums.TicketStatus;
import com.pulsepass.enums.TicketType;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserProfileRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de seguridad con la aplicacion completa: cadena de filtros real,
 * filtro JWT real, @PreAuthorize real y PostgreSQL en Testcontainers.
 *
 * Cubre las 22 rutas de la API por rol y los casos negativos del filtro JWT.
 */
@SpringBootTest(properties = {
        "pulsepass.security.jwt.secret-base64=" + SecurityIT.SECRET,
        "pulsepass.security.jwt.issuer=pulsepass-security-it",
        "pulsepass.security.jwt.access-token-ttl=PT15M"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class SecurityIT {

    static final String SECRET = "c2VjdXJpdHktaXQtc2VjcmV0LWtleS1mb3ItdGVzdHMtb25seS0wMTIzNDU2Nzg5";
    private static final String ISSUER = "pulsepass-security-it";

    /** El actor tiene acceso concedido: la respuesta no es 401 ni 403. */
    private static final int ALLOWED = -1;

    private static final String OWNER_EMAIL = "owner@sec.test";
    private static final String OWNER_USERNAME = "sec_owner";

    private static Long artistId;

    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired UserProfileRepository userProfileRepository;
    @Autowired UserCredentialRepository credentialRepository;
    @Autowired UserRoleRepository roleRepository;
    @Autowired VenueRepository venueRepository;
    @Autowired EventRepository eventRepository;
    @Autowired TicketRepository ticketRepository;
    @Autowired ArtistRepository artistRepository;

    private User owner;
    private User other;
    private User staff;
    private User admin;

    enum Actor { ANON, OWNER, OTHER, STAFF, ADMIN }

    @BeforeEach
    void seed() {
        owner = createUser(OWNER_USERNAME, OWNER_EMAIL, true, "USER");
        other = createUser("sec_other", "other@sec.test", true, "USER");
        staff = createUser("sec_staff", "staff@sec.test", true, "STAFF");
        admin = createUser("sec_admin", "admin@sec.test", true, "ADMIN");

        Venue venue = new Venue();
        venue.setCode("SEC-VEN");
        venue.setName("Security Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Calle 1");
        venue.setCapacity(100);
        venue.setActive(true);
        venueRepository.saveAndFlush(venue);

        Artist artist = artistRepository.findByStageName("Solar Beat").orElseThrow();
        artistId = artist.getId();

        Event published = createEvent("SEC-PUB", EventStatus.PUBLISHED, venue);
        published.getArtists().add(artist);
        Event draft = createEvent("SEC-DRAFT", EventStatus.DRAFT, venue);
        draft.getArtists().add(artist);
        eventRepository.saveAllAndFlush(java.util.List.of(published, draft));

        Ticket ticket = new Ticket();
        ticket.setTicketCode("SEC-TCK-1");
        ticket.setType(TicketType.GENERAL);
        ticket.setPrice(new BigDecimal("120000.00"));
        ticket.setStatus(TicketStatus.PAID);
        ticket.setPurchaseDate(LocalDate.now());
        ticket.setUser(owner);
        ticket.setEvent(published);
        ticketRepository.saveAndFlush(ticket);
    }

    // ------------------------------------------------------------------
    // Las 22 rutas de la API, por rol
    // ------------------------------------------------------------------

    static Stream<Arguments> routeMatrix() {
        Stream.Builder<Arguments> b = Stream.builder();

        // --- Publicas (permitAll) ---
        add(b, "POST", "/api/users", "{}", Actor.ANON, 400);
        add(b, "POST", "/api/auth/login", "{}", Actor.ANON, 400);
        add(b, "GET", "/api/venues/active", null, Actor.ANON, 200);
        add(b, "GET", "/api/venues/SEC-VEN", null, Actor.ANON, 200);
        add(b, "GET", "/api/artists/active", null, Actor.ANON, 200);
        add(b, "GET", "/api/artists/by-stage-name?stageName=Solar Beat", null, Actor.ANON, 200);
        add(b, "GET", "/api/artists/{artistId}", null, Actor.ANON, 200);
        add(b, "GET", "/api/events/published", null, Actor.ANON, 200);
        add(b, "GET", "/api/events/by-artist?stageName=Solar Beat", null, Actor.ANON, 200);

        // Detalle de evento: publicado es publico; borrador solo STAFF/ADMIN.
        add(b, "GET", "/api/events/SEC-PUB", null, Actor.ANON, 200);
        add(b, "GET", "/api/events/SEC-PUB", null, Actor.OWNER, 200);
        add(b, "GET", "/api/events/SEC-DRAFT", null, Actor.ANON, 403);
        add(b, "GET", "/api/events/SEC-DRAFT", null, Actor.OWNER, 403);
        add(b, "GET", "/api/events/SEC-DRAFT", null, Actor.STAFF, 200);
        add(b, "GET", "/api/events/SEC-DRAFT", null, Actor.ADMIN, 200);

        // --- Autenticacion ---
        add(b, "GET", "/api/auth/me", null, Actor.ANON, 401);
        add(b, "GET", "/api/auth/me", null, Actor.OWNER, 200);
        add(b, "GET", "/api/auth/me", null, Actor.STAFF, 200);
        add(b, "GET", "/api/auth/me", null, Actor.ADMIN, 200);

        // --- Administracion de eventos: solo ADMIN ---
        add(b, "POST", "/api/events", "{}", Actor.ANON, 401);
        add(b, "POST", "/api/events", "{}", Actor.OWNER, 403);
        add(b, "POST", "/api/events", "{}", Actor.STAFF, 403);
        add(b, "POST", "/api/events", "{}", Actor.ADMIN, 400);

        add(b, "PATCH", "/api/events/SEC-DRAFT/publish", null, Actor.ANON, 401);
        add(b, "PATCH", "/api/events/SEC-DRAFT/publish", null, Actor.OWNER, 403);
        add(b, "PATCH", "/api/events/SEC-DRAFT/publish", null, Actor.STAFF, 403);
        add(b, "PATCH", "/api/events/SEC-DRAFT/publish", null, Actor.ADMIN, ALLOWED);

        add(b, "POST", "/api/events/SEC-PUB/artists/{artistId}", null, Actor.ANON, 401);
        add(b, "POST", "/api/events/SEC-PUB/artists/{artistId}", null, Actor.OWNER, 403);
        add(b, "POST", "/api/events/SEC-PUB/artists/{artistId}", null, Actor.STAFF, 403);
        add(b, "POST", "/api/events/SEC-PUB/artists/{artistId}", null, Actor.ADMIN, ALLOWED);

        // --- Tickets pagados de un evento: STAFF y ADMIN ---
        add(b, "GET", "/api/events/SEC-PUB/tickets/paid", null, Actor.ANON, 401);
        add(b, "GET", "/api/events/SEC-PUB/tickets/paid", null, Actor.OWNER, 403);
        add(b, "GET", "/api/events/SEC-PUB/tickets/paid", null, Actor.STAFF, 200);
        add(b, "GET", "/api/events/SEC-PUB/tickets/paid", null, Actor.ADMIN, 200);

        // --- Compra: solo USER, y solo a su propio correo ---
        String purchase = "{\"userEmail\":\"" + OWNER_EMAIL + "\",\"eventCode\":\"SEC-PUB\",\"type\":\"VIP\"}";
        add(b, "POST", "/api/tickets", purchase, Actor.ANON, 401);
        add(b, "POST", "/api/tickets", purchase, Actor.OTHER, 403);
        add(b, "POST", "/api/tickets", purchase, Actor.STAFF, 403);
        add(b, "POST", "/api/tickets", purchase, Actor.ADMIN, 403);
        add(b, "POST", "/api/tickets", purchase, Actor.OWNER, 201);

        // --- Consulta de un ticket: propietario, STAFF o ADMIN ---
        add(b, "GET", "/api/tickets/SEC-TCK-1", null, Actor.ANON, 401);
        add(b, "GET", "/api/tickets/SEC-TCK-1", null, Actor.OWNER, 200);
        add(b, "GET", "/api/tickets/SEC-TCK-1", null, Actor.OTHER, 403);
        add(b, "GET", "/api/tickets/SEC-TCK-1", null, Actor.STAFF, 200);
        add(b, "GET", "/api/tickets/SEC-TCK-1", null, Actor.ADMIN, 200);

        // --- Tickets por correo: propietario o ADMIN (STAFF no) ---
        String byUser = "/api/tickets/by-user?email=" + OWNER_EMAIL;
        add(b, "GET", byUser, null, Actor.ANON, 401);
        add(b, "GET", byUser, null, Actor.OWNER, 200);
        add(b, "GET", byUser, null, Actor.OTHER, 403);
        add(b, "GET", byUser, null, Actor.STAFF, 403);
        add(b, "GET", byUser, null, Actor.ADMIN, 200);

        // --- Cancelacion: propietario o ADMIN ---
        add(b, "PATCH", "/api/tickets/SEC-TCK-1/cancel", null, Actor.ANON, 401);
        add(b, "PATCH", "/api/tickets/SEC-TCK-1/cancel", null, Actor.OTHER, 403);
        add(b, "PATCH", "/api/tickets/SEC-TCK-1/cancel", null, Actor.STAFF, 403);
        add(b, "PATCH", "/api/tickets/SEC-TCK-1/cancel", null, Actor.OWNER, ALLOWED);
        add(b, "PATCH", "/api/tickets/SEC-TCK-1/cancel", null, Actor.ADMIN, ALLOWED);

        // --- Uso de ticket: STAFF o ADMIN ---
        add(b, "PATCH", "/api/tickets/SEC-TCK-1/use", null, Actor.ANON, 401);
        add(b, "PATCH", "/api/tickets/SEC-TCK-1/use", null, Actor.OWNER, 403);
        add(b, "PATCH", "/api/tickets/SEC-TCK-1/use", null, Actor.STAFF, ALLOWED);
        add(b, "PATCH", "/api/tickets/SEC-TCK-1/use", null, Actor.ADMIN, ALLOWED);

        // --- Consulta de usuarios: el propio usuario o ADMIN ---
        for (String route : new String[]{
                "/api/users/by-email?email=" + OWNER_EMAIL,
                "/api/users/by-username?username=" + OWNER_USERNAME}) {
            add(b, "GET", route, null, Actor.ANON, 401);
            add(b, "GET", route, null, Actor.OWNER, 200);
            add(b, "GET", route, null, Actor.OTHER, 403);
            add(b, "GET", route, null, Actor.STAFF, 403);
            add(b, "GET", route, null, Actor.ADMIN, 200);
        }

        return b.build();
    }

    private static void add(Stream.Builder<Arguments> b, String method, String path,
                            String body, Actor actor, int expected) {
        b.add(Arguments.of(method, path, body, actor, expected));
    }

    @ParameterizedTest(name = "{3} {0} {1} -> {4}")
    @MethodSource("routeMatrix")
    void routeIsProtectedAsSpecified(String method, String path, String body,
                                     Actor actor, int expected) throws Exception {
        String resolved = path.replace("{artistId}", String.valueOf(artistId));

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .request(org.springframework.http.HttpMethod.valueOf(method), resolved);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        String token = tokenFor(actor);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }

        int actual = mvc.perform(request).andReturn().getResponse().getStatus();

        if (expected == ALLOWED) {
            assertThat(actual).as("el acceso debia concederse (no 401 ni 403)")
                    .isNotIn(401, 403);
        } else {
            assertThat(actual).isEqualTo(expected);
        }
    }

    // ------------------------------------------------------------------
    // Visibilidad segun rol en /api/events/by-artist
    // ------------------------------------------------------------------

    @Test
    void byArtist_publicSeesOnlyPublishedEvents() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/events/by-artist")
                        .param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventCode").value("SEC-PUB"));
    }

    @Test
    void byArtist_staffSeesEventsInEveryStatus() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/events/by-artist")
                        .param("stageName", "Solar Beat")
                        .header("Authorization", "Bearer " + tokenFor(Actor.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ------------------------------------------------------------------
    // Filtro JWT: casos negativos (ruta protegida: GET /api/auth/me)
    // ------------------------------------------------------------------

    @Test
    void validToken_isAccepted() throws Exception {
        mvc.perform(me().header("Authorization", "Bearer " + tokenFor(Actor.OWNER)))
                .andExpect(status().isOk());
    }

    @Test
    void missingToken_returns401WithErrorBody() throws Exception {
        mvc.perform(me())
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void nonBearerScheme_returns401() throws Exception {
        mvc.perform(me().header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emptyBearerToken_returns401() throws Exception {
        mvc.perform(me().header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedToken_returns401() throws Exception {
        mvc.perform(me().header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicatedAuthorizationHeader_returns401() throws Exception {
        String token = tokenFor(Actor.OWNER);
        mvc.perform(me()
                        .header("Authorization", "Bearer " + token)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredToken_returns401() throws Exception {
        Instant now = Instant.now();
        String token = builder(owner.getId().toString())
                .issuedAt(Date.from(now.minusSeconds(7200)))
                .expiration(Date.from(now.minusSeconds(3600)))
                .signWith(key(), Jwts.SIG.HS256).compact();

        mvc.perform(me().header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherKey_returns401() throws Exception {
        SecretKey other = Keys.hmacShaKeyFor(new byte[48]);
        String token = builder(owner.getId().toString()).signWith(other, Jwts.SIG.HS256).compact();

        mvc.perform(me().header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithNonNumericSubject_returns401() throws Exception {
        String token = builder("not-a-number").signWith(key(), Jwts.SIG.HS256).compact();

        mvc.perform(me().header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenOfDeletedUser_returns401() throws Exception {
        String token = jwtService.generateToken("999999999");

        mvc.perform(me().header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenOfInactiveUser_returns401() throws Exception {
        String token = tokenFor(Actor.OWNER);
        owner.setActive(false);
        userRepository.saveAndFlush(owner);

        mvc.perform(me().header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenOfUserWithoutRoles_returns401() throws Exception {
        User noRoles = createUser("sec_norole", "norole@sec.test", true);
        String token = jwtService.generateToken(noRoles.getId().toString());

        mvc.perform(me().header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenOnPublicRoute_isStillRejectedWith401() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/venues/active")
                        .header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleRevokedAfterIssuingToken_takesEffectImmediately() throws Exception {
        String token = tokenFor(Actor.ADMIN);
        roleRepository.deleteAll(roleRepository.findAllByUserId(admin.getId()));
        roleRepository.save(new UserRole(admin.getId(), "USER"));
        roleRepository.flush();

        mvc.perform(MockMvcRequestBuilders.post("/api/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private MockHttpServletRequestBuilder me() {
        return MockMvcRequestBuilders.get("/api/auth/me");
    }

    private String tokenFor(Actor actor) {
        User user = switch (actor) {
            case ANON -> null;
            case OWNER -> owner;
            case OTHER -> other;
            case STAFF -> staff;
            case ADMIN -> admin;
        };
        return user == null ? null : jwtService.generateToken(user.getId().toString());
    }

    private io.jsonwebtoken.JwtBuilder builder(String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuer(ISSUER)
                .id(java.util.UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(600)));
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }

    private User createUser(String username, String email, boolean active, String... roles) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setActive(active);
        userRepository.saveAndFlush(user);

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setFirstName("Sec");
        profile.setLastName("Tester");
        profile.setBirthDate(LocalDate.of(1995, 1, 1));
        userProfileRepository.saveAndFlush(profile);

        credentialRepository.saveAndFlush(new UserCredential(user, "$2a$10$notARealHashJustForTests"));
        for (String role : roles) {
            roleRepository.saveAndFlush(new UserRole(user.getId(), role));
        }
        return user;
    }

    private Event createEvent(String code, EventStatus status, Venue venue) {
        Event event = new Event();
        event.setEventCode(code);
        event.setName("Evento " + code);
        event.setDescription("Evento de pruebas de seguridad");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(status);
        event.setEventDate(LocalDate.now().plusDays(30));
        event.setMinimumAge(0);
        event.setVenue(venue);
        return event;
    }
}
