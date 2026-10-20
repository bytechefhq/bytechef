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

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceRepository;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationInstanceEnvironmentResolverTest {

    private final IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository =
        mock(IntegrationInstanceConfigurationRepository.class);
    private final IntegrationInstanceRepository integrationInstanceRepository =
        mock(IntegrationInstanceRepository.class);
    private final IntegrationInstanceEnvironmentResolver resolver = new IntegrationInstanceEnvironmentResolver(
        integrationInstanceConfigurationRepository, integrationInstanceRepository);

    @Test
    void testResourceTypeMatchesTheGateToken() {
        assertThat(resolver.resourceType()).isEqualTo("IntegrationInstance");
    }

    @Test
    void testTheEnvironmentComesFromTheParentConfiguration() {
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setIntegrationInstanceConfigurationId(11L);

        IntegrationInstanceConfiguration configuration = new IntegrationInstanceConfiguration();

        configuration.setEnvironment(Environment.PRODUCTION);

        when(integrationInstanceRepository.findById(7L)).thenReturn(Optional.of(integrationInstance));
        when(integrationInstanceConfigurationRepository.findById(11L)).thenReturn(Optional.of(configuration));

        assertThat(resolver.fetchEnvironment(7L)).contains(Environment.PRODUCTION);
    }

    @Test
    void testAMissingRowHasNoEnvironment() {
        when(integrationInstanceRepository.findById(7L)).thenReturn(Optional.empty());

        assertThat(resolver.fetchEnvironment(7L)).isEmpty();
    }

    @Test
    void testAMissingParentConfigurationHasNoEnvironment() {
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setIntegrationInstanceConfigurationId(11L);

        when(integrationInstanceRepository.findById(7L)).thenReturn(Optional.of(integrationInstance));
        when(integrationInstanceConfigurationRepository.findById(11L)).thenReturn(Optional.empty());

        assertThat(resolver.fetchEnvironment(7L)).isEmpty();
    }
}
