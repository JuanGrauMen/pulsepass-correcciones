package com.pulsepass.dev;

import com.pulsepass.TestcontainersConfiguration;
import com.pulsepass.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sin el perfil "dev" la semilla no existe: ninguna cuenta privilegiada se
 * crea al arrancar la aplicacion en un entorno normal.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DevSeedAbsentWithoutProfileIT {

    @Autowired ApplicationContext context;
    @Autowired UserRepository userRepository;

    @Test
    void seederBeanIsNotRegistered() {
        assertThat(context.getBeanNamesForType(DevDataSeeder.class)).isEmpty();
    }

    @Test
    void noPrivilegedAccountExistsAfterStartup() {
        assertThat(userRepository.existsByUsername("admin_dev")).isFalse();
        assertThat(userRepository.existsByUsername("staff_dev")).isFalse();
    }
}
