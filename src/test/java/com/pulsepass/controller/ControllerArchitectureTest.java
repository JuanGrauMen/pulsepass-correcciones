package com.pulsepass.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regla de arquitectura declarada en el README: los controllers delegan en
 * los services y no acceden directamente a repositories ni a entidades JPA
 * como dependencias.
 */
class ControllerArchitectureTest {

    private static final String REPOSITORY_PACKAGE = "com.pulsepass.repository";

    private Set<Class<?>> controllers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        Set<Class<?>> result = new java.util.HashSet<>();
        for (BeanDefinition definition : scanner.findCandidateComponents("com.pulsepass.controller")) {
            result.add(Class.forName(definition.getBeanClassName()));
        }
        return result;
    }

    @Test
    void thereAreControllersToCheck() throws Exception {
        assertThat(controllers()).hasSizeGreaterThanOrEqualTo(6);
    }

    @Test
    void controllers_doNotDependOnRepositories() throws Exception {
        List<String> violations = new ArrayList<>();

        for (Class<?> controller : controllers()) {
            for (Field field : controller.getDeclaredFields()) {
                if (field.getType().getPackageName().startsWith(REPOSITORY_PACKAGE)) {
                    violations.add(controller.getSimpleName() + " campo " + field.getName());
                }
            }
            for (var constructor : controller.getDeclaredConstructors()) {
                for (Parameter parameter : constructor.getParameters()) {
                    if (parameter.getType().getPackageName().startsWith(REPOSITORY_PACKAGE)) {
                        violations.add(controller.getSimpleName() + " constructor "
                                + parameter.getType().getSimpleName());
                    }
                }
            }
        }

        assertThat(violations)
                .as("Los controllers no deben depender de repositories: %s",
                        violations.stream().collect(Collectors.joining(", ")))
                .isEmpty();
    }
}
