package com.pulsepass.dto;

import com.pulsepass.dto.request.RegisterUserRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reglas de contrasena del registro: minimo 12 caracteres y maximo 72 bytes
 * UTF-8 (limite de BCrypt). Todas las violaciones son errores de validacion
 * del DTO y por tanto responden 400.
 */
class RegisterUserRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private Set<ConstraintViolation<RegisterUserRequest>> validate(String password) {
        return validator.validate(new RegisterUserRequest(
                "andrea", "andrea@email.com", "Andrea", "Diaz",
                null, null, LocalDate.of(1998, 5, 20), password));
    }

    private boolean rejectsPassword(String password) {
        return validate(password).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    void password_withElevenCharacters_isRejected() {
        assertThat(rejectsPassword("a".repeat(11))).isTrue();
    }

    @Test
    void password_withTwelveCharacters_isAccepted() {
        assertThat(rejectsPassword("a".repeat(12))).isFalse();
    }

    @Test
    void password_of72AsciiBytes_isAccepted() {
        assertThat(rejectsPassword("a".repeat(72))).isFalse();
    }

    @Test
    void password_of73AsciiBytes_isRejected() {
        assertThat(rejectsPassword("a".repeat(73))).isTrue();
    }

    @Test
    void password_limitIsMeasuredInBytesNotCharacters() {
        // "e" con tilde ocupa 2 bytes en UTF-8: 36 caracteres = 72 bytes.
        assertThat(rejectsPassword("é".repeat(36))).isFalse();
        // 37 caracteres = 74 bytes, aunque solo sean 37 caracteres.
        assertThat(rejectsPassword("é".repeat(37))).isTrue();
    }

    @Test
    void password_withEmoji_countsFourBytesEach() {
        // 18 emojis = 72 bytes (y 36 chars Java); 19 = 76 bytes.
        assertThat(rejectsPassword("😀".repeat(18))).isFalse();
        assertThat(rejectsPassword("😀".repeat(19))).isTrue();
    }

    @Test
    void password_blankOrNull_isRejected() {
        assertThat(rejectsPassword("            ")).isTrue();
        assertThat(rejectsPassword(null)).isTrue();
    }

    @Test
    void validRequest_hasNoViolations() {
        assertThat(validate("PulsePassTest!2026")).isEmpty();
    }
}
