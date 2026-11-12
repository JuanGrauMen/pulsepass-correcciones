package com.pulsepass.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * El texto no puede ocupar mas de {@code value} bytes en UTF-8.
 *
 * Se usa para el limite de 72 bytes de BCrypt, que cuenta bytes y no
 * caracteres: un caracter acentuado ocupa 2 bytes y un emoji 4. Un valor nulo
 * se considera valido; la presencia se exige con @NotBlank.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
public @interface MaxUtf8Bytes {

    int value();

    String message() default "Must not exceed {value} bytes when encoded as UTF-8";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
