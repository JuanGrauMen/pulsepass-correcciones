package com.pulsepass;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class FlywayMigrationIT {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.pulsepass.repository.VenueRepository venueRepository;

    @Test
    void shouldApplyAllMigrationsAndCreateRequiredIntegrityRules() {
        Set<String> appliedVersions = Arrays.stream(flyway.info().applied())
                .map(MigrationInfo::getVersion)
                .filter(Objects::nonNull)
                .map(version -> version.getVersion())
                .collect(Collectors.toSet());

        assertTrue(appliedVersions.containsAll(Set.of("1", "2", "3", "4")),
                "Flyway should apply V1 through V4 to an empty PostgreSQL database");

        Integer streamingUrlColumnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'events'
                  AND column_name = 'streaming_url'
                """, Integer.class);
        assertEquals(1, streamingUrlColumnCount);

        Integer checkConstraintCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE constraint_schema = 'public'
                  AND constraint_name IN (
                    'chk_events_category',
                    'chk_events_status',
                    'chk_events_minimum_age',
                    'chk_tickets_type',
                    'chk_tickets_status'
                  )
                """, Integer.class);
        assertEquals(5, checkConstraintCount);

        Integer emailIndexCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = 'ux_users_email_lower'
                """, Integer.class);
        assertEquals(1, emailIndexCount);
    }

    @Test
    void databaseShouldRejectAnEventWithAnUnsupportedCategory() {
        com.pulsepass.entity.Venue venue = new com.pulsepass.entity.Venue();
        venue.setCode("VEN-CHECK-CATEGORY");
        venue.setName("Constraint Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Test address");
        venue.setCapacity(100);
        venue.setActive(true);
        venue = venueRepository.saveAndFlush(venue);

        Long venueId = venue.getId();
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
                jdbcTemplate.update("""
                        INSERT INTO events
                            (event_code, name, category, status, event_date, minimum_age, venue_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                        "EV-INVALID-CATEGORY",
                        "Invalid category",
                        "NOT_A_CATEGORY",
                        "DRAFT",
                        Date.valueOf(LocalDate.now().plusDays(5)),
                        0,
                        venueId
                ));
    }
}
