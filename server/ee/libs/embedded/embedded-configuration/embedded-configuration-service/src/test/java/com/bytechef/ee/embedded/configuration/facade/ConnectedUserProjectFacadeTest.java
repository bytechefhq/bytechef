/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class ConnectedUserProjectFacadeTest {

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Mock
    private IntegrationService integrationService;

    private ConnectedUserProjectFacadeImpl facade;

    @BeforeEach
    void setUp() {
        facade = new ConnectedUserProjectFacadeImpl(
            null, componentDefinitionService, null, null, null, null, null, null, null,
            integrationInstanceConfigurationService, integrationService, null, null, null, null, null, null, null,
            null, null, null, null, null, null);
    }

    @Test
    void testResolveAllowedComponentNamesUnionsEnabledIntegrationsAndConnectionlessComponents() {
        IntegrationInstanceConfiguration enabledConfiguration = new IntegrationInstanceConfiguration();

        enabledConfiguration.setIntegrationId(10L);

        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(Environment.PRODUCTION, true))
            .thenReturn(List.of(enabledConfiguration));

        Integration slackIntegration = new Integration();

        slackIntegration.setComponentName("slack");

        when(integrationService.getIntegrations(List.of(10L))).thenReturn(List.of(slackIntegration));

        // logger requires no connection (kept); httpClient requires a connection (filtered out unless enabled)
        ComponentDefinition loggerDefinition = mock(ComponentDefinition.class);

        when(loggerDefinition.getName()).thenReturn("logger");
        when(loggerDefinition.isConnectionRequired()).thenReturn(false);

        ComponentDefinition httpDefinition = mock(ComponentDefinition.class);

        when(httpDefinition.isConnectionRequired()).thenReturn(true);

        when(componentDefinitionService.getComponentDefinitions())
            .thenReturn(List.of(loggerDefinition, httpDefinition));

        Set<String> result = facade.resolveAllowedComponentNames(Environment.PRODUCTION);

        assertThat(result).containsExactlyInAnyOrder("slack", "logger");
    }

    @Test
    void testResolveAllowedComponentNamesWithNoEnabledIntegrationsKeepsOnlyConnectionlessComponents() {
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(Environment.PRODUCTION, true))
            .thenReturn(List.of());
        when(integrationService.getIntegrations(List.of())).thenReturn(List.of());

        ComponentDefinition loggerDefinition = mock(ComponentDefinition.class);

        when(loggerDefinition.getName()).thenReturn("logger");
        when(loggerDefinition.isConnectionRequired()).thenReturn(false);

        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(loggerDefinition));

        Set<String> result = facade.resolveAllowedComponentNames(Environment.PRODUCTION);

        assertThat(result).containsExactly("logger");
    }

    @Test
    void testResolveAllowedComponentNamesSkipsConfigurationsWithNullIntegrationIdAndNullComponentName() {
        IntegrationInstanceConfiguration unlinkedConfiguration = new IntegrationInstanceConfiguration();

        IntegrationInstanceConfiguration enabledConfiguration = new IntegrationInstanceConfiguration();

        enabledConfiguration.setIntegrationId(20L);

        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(Environment.PRODUCTION, true))
            .thenReturn(List.of(unlinkedConfiguration, enabledConfiguration));

        Integration namelessIntegration = new Integration();

        Integration slackIntegration = new Integration();

        slackIntegration.setComponentName("slack");

        // Only the non-null integrationId (20L) reaches getIntegrations; the integration with a null
        // componentName must be filtered out rather than polluting the allow-list with null.
        when(integrationService.getIntegrations(List.of(20L)))
            .thenReturn(List.of(namelessIntegration, slackIntegration));

        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of());

        Set<String> result = facade.resolveAllowedComponentNames(Environment.PRODUCTION);

        assertThat(result).containsExactly("slack");
    }
}
