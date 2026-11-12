package com.pulsepass;

import com.pulsepass.entity.User;
import com.pulsepass.entity.UserProfile;
import com.pulsepass.repository.UserProfileRepository;
import com.pulsepass.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserProfileIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    void shouldPersistUserWithUserProfile() {

        User user = new User();

        user.setUsername("profile-user-01");
        user.setEmail("profile01@test.com");
        user.setActive(true);

        user = userRepository.saveAndFlush(user);

        UserProfile profile = new UserProfile();

        profile.setFirstName("Sara");
        profile.setLastName("Test");
        profile.setPhone("3001234567");
        profile.setCity("Santa Marta");
        profile.setBirthDate(LocalDate.of(2000, 1, 15));
        profile.setUser(user);

        UserProfile savedProfile = userProfileRepository.saveAndFlush(profile);
        assertNotNull(savedProfile.getId());

        UserProfile result = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AssertionError("No se encontró el perfil guardado"));

        assertEquals(user.getId(), result.getUser().getId());
        assertEquals("Sara", result.getFirstName());
        assertEquals("Test", result.getLastName());
        assertEquals("Santa Marta", result.getCity());
        assertEquals(LocalDate.of(2000, 1, 15), result.getBirthDate());
    }

    @Test
    void shouldRejectSecondProfileForSameUser() {
        User user = new User();
        user.setUsername("profile-unique-user");
        user.setEmail("profile-unique@test.com");
        user.setActive(true);
        user = userRepository.saveAndFlush(user);

        UserProfile first = new UserProfile();
        first.setFirstName("First");
        first.setLastName("Profile");
        first.setUser(user);
        userProfileRepository.saveAndFlush(first);

        UserProfile second = new UserProfile();
        second.setFirstName("Second");
        second.setLastName("Profile");
        second.setUser(user);

        assertThrows(DataIntegrityViolationException.class,
                () -> userProfileRepository.saveAndFlush(second));
    }


    @Test
    void shouldRejectDuplicateEmailsIgnoringCase() {
        User first = new User();
        first.setUsername("email-case-user-1");
        first.setEmail("CaseCheck@test.com");
        first.setActive(true);
        userRepository.saveAndFlush(first);

        User duplicate = new User();
        duplicate.setUsername("email-case-user-2");
        duplicate.setEmail("casecheck@TEST.com");
        duplicate.setActive(true);

        assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(duplicate));
    }


    @Test
    void shouldRejectDuplicateUsername() {
        User first = new User();
        first.setUsername("duplicate-username");
        first.setEmail("username-first@test.com");
        first.setActive(true);
        userRepository.saveAndFlush(first);

        User duplicate = new User();
        duplicate.setUsername("duplicate-username");
        duplicate.setEmail("username-second@test.com");
        duplicate.setActive(true);

        assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(duplicate));
    }

}