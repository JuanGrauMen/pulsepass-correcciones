package com.pulsepass;

import com.pulsepass.entity.Venue;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class VenuePersistenceIT {

    @Autowired
    private VenueRepository venueRepository;

    @Test
    void shouldPersistAndFindVenueByCode() {

        Venue venue = new Venue();

        venue.setCode("VEN-PERSIST-01");
        venue.setName("Persistence Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Test Address");
        venue.setCapacity(5000);
        venue.setActive(true);

        venueRepository.saveAndFlush(venue);

        Venue result =
                venueRepository.findByCode("VEN-PERSIST-01")
                        .orElseThrow();

        assertEquals("VEN-PERSIST-01", result.getCode());
        assertEquals("Santa Marta", result.getCity());
        assertEquals(5000, result.getCapacity());
    }

    @Test
    void shouldRejectDuplicateVenueCode() {

        Venue first = new Venue();

        first.setCode("VEN-UNIQUE-01");
        first.setName("First Venue");
        first.setCity("Santa Marta");
        first.setAddress("Address 1");
        first.setCapacity(1000);
        first.setActive(true);

        venueRepository.saveAndFlush(first);

        Venue duplicate = new Venue();

        duplicate.setCode("VEN-UNIQUE-01");
        duplicate.setName("Duplicate Venue");
        duplicate.setCity("Santa Marta");
        duplicate.setAddress("Address 2");
        duplicate.setCapacity(2000);
        duplicate.setActive(true);

        assertThrows(
                Exception.class,
                () -> venueRepository.saveAndFlush(duplicate)
        );
    }

    @Test
    void shouldRejectNonPositiveCapacity() {
        Venue invalid = new Venue();
        invalid.setCode("VEN-INVALID-CAPACITY");
        invalid.setName("Invalid Capacity Venue");
        invalid.setCity("Santa Marta");
        invalid.setAddress("Test Address");
        invalid.setCapacity(0);
        invalid.setActive(true);

        assertThrows(DataIntegrityViolationException.class,
                () -> venueRepository.saveAndFlush(invalid));
    }

}