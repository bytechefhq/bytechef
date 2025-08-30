/*
 * Copyright 2025 ByteChef
 */
package com.bytechef.platform.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.Test;

import java.util.List;

public class EnvironmentServiceTest {

    private final EnvironmentService environmentService = new EnvironmentServiceImpl();

    @Test
    public void testGetEnvironments() {
        List<Environment> environments = environmentService.getEnvironments();

        assertThat(environments).containsExactly(Environment.DEVELOPMENT, Environment.STAGING, Environment.PRODUCTION);
    }

    @Test
    public void testGetEnvironmentByName() {
        assertThat(environmentService.getEnvironment("development")).isEqualTo(Environment.DEVELOPMENT);
        assertThat(environmentService.getEnvironment("STAGING")).isEqualTo(Environment.STAGING);
        assertThat(environmentService.getEnvironment("Production")).isEqualTo(Environment.PRODUCTION);
    }

    @Test
    public void testGetEnvironmentById() {
        assertThat(environmentService.getEnvironment(0)).isEqualTo(Environment.DEVELOPMENT);
        assertThat(environmentService.getEnvironment(1)).isEqualTo(Environment.STAGING);
        assertThat(environmentService.getEnvironment(2)).isEqualTo(Environment.PRODUCTION);
    }

    @Test
    public void testInvalidNameThrows() {
        assertThatThrownBy(() -> environmentService.getEnvironment("invalid"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testInvalidIdThrows() {
        assertThatThrownBy(() -> environmentService.getEnvironment(-1))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> environmentService.getEnvironment(3))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
