/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationInstanceConfigurationEnvironmentResolverTest {

    private final IntegrationInstanceConfigurationRepository repository =
        mock(IntegrationInstanceConfigurationRepository.class);
    private final IntegrationInstanceConfigurationEnvironmentResolver resolver =
        new IntegrationInstanceConfigurationEnvironmentResolver(repository);

    @Test
    void testResourceTypeMatchesTheGateToken() {
        assertThat(resolver.resourceType()).isEqualTo("IntegrationInstanceConfiguration");
    }

    @Test
    void testTheEnvironmentComesFromTheEnvironmentColumn() {
        IntegrationInstanceConfiguration configuration = new IntegrationInstanceConfiguration();

        configuration.setEnvironment(Environment.STAGING);

        when(repository.findById(7L)).thenReturn(Optional.of(configuration));

        assertThat(resolver.fetchEnvironment(7L)).contains(Environment.STAGING);
    }

    @Test
    void testAMissingRowHasNoEnvironment() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThat(resolver.fetchEnvironment(7L)).isEmpty();
    }

    @Test
    void testANonNumericIdHasNoEnvironment() {
        assertThat(resolver.fetchEnvironment("not-a-number")).isEmpty();
    }
}
