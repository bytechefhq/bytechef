/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.teardown;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceConfigurationFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationInstanceTenantTeardownContributorTest {

    private final IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade =
        mock(IntegrationInstanceConfigurationFacade.class);
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService =
        mock(IntegrationInstanceConfigurationService.class);
    private final IntegrationInstanceTenantTeardownContributor contributor =
        new IntegrationInstanceTenantTeardownContributor(
            integrationInstanceConfigurationFacade, integrationInstanceConfigurationService);

    @Test
    void testDisablesEveryEnabledConfiguration() {
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations())
            .thenReturn(
                List.of(
                    createIntegrationInstanceConfiguration(1L, true),
                    createIntegrationInstanceConfiguration(2L, false),
                    createIntegrationInstanceConfiguration(3L, true)));

        assertThat(contributor.onDeletionRequested("000001")).isEmpty();

        verify(integrationInstanceConfigurationFacade).enableIntegrationInstanceConfiguration(1L, false);
        verify(integrationInstanceConfigurationFacade, never()).enableIntegrationInstanceConfiguration(2L, false);
        verify(integrationInstanceConfigurationFacade).enableIntegrationInstanceConfiguration(3L, false);
    }

    @Test
    void testFailureOnOneConfigurationDoesNotStopTheNext() {
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations())
            .thenReturn(
                List.of(
                    createIntegrationInstanceConfiguration(1L, true),
                    createIntegrationInstanceConfiguration(3L, true)));

        doThrow(new IllegalStateException("trigger disable failed"))
            .when(integrationInstanceConfigurationFacade)
            .enableIntegrationInstanceConfiguration(1L, false);

        assertThat(contributor.onDeletionRequested("000001"))
            .containsExactly("integrationInstanceConfiguration 1: trigger disable failed");

        verify(integrationInstanceConfigurationFacade).enableIntegrationInstanceConfiguration(3L, false);
    }

    private static IntegrationInstanceConfiguration createIntegrationInstanceConfiguration(long id, boolean enabled) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setEnabled(enabled);
        integrationInstanceConfiguration.setId(id);

        return integrationInstanceConfiguration;
    }
}
