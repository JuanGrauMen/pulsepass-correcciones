package com.pulsepass.dev;

import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserProfileRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * La semilla exige una contrasena explicita: no existe un valor por defecto,
 * asi que ninguna cuenta privilegiada puede quedar con una clave conocida.
 */
class DevDataSeederPasswordTest {

    private final VenueRepository venues = mock(VenueRepository.class);
    private final EventRepository events = mock(EventRepository.class);
    private final ArtistRepository artists = mock(ArtistRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final UserProfileRepository profiles = mock(UserProfileRepository.class);
    private final UserCredentialRepository credentials = mock(UserCredentialRepository.class);
    private final UserRoleRepository roles = mock(UserRoleRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);

    private DevDataSeeder seeder(String password) {
        return new DevDataSeeder(venues, events, artists, users, profiles,
                credentials, roles, encoder, password);
    }

    @Test
    void blankPassword_failsWithAClearMessageAndWritesNothing() {
        assertThatThrownBy(() -> seeder("").seed())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PULSEPASS_DEV_SEED_PASSWORD");

        verifyNoInteractions(venues, events, users, credentials, roles);
    }

    @Test
    void nullPassword_failsWithAClearMessage() {
        assertThatThrownBy(() -> seeder(null).seed())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PULSEPASS_DEV_SEED_PASSWORD");
    }

    @Test
    void passwordShorterThan12_isRejected() {
        assertThatThrownBy(() -> seeder("short-pass").seed())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("12");

        verifyNoInteractions(users, credentials, roles);
    }

    @Test
    void passwordOver72Bytes_isRejected() {
        assertThatThrownBy(() -> seeder("a".repeat(73)).seed())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("72");

        verifyNoInteractions(users, credentials, roles);
    }
}
