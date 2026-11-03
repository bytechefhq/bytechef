/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.instance.accessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalSummary;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationJobPrincipalAccessorTest {

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService =
        mock(IntegrationInstanceConfigurationService.class);
    private final IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
    private final IntegrationService integrationService = mock(IntegrationService.class);

    @Test
    void testGetJobPrincipalSummaryHasNoNameWhenTheIntegrationIsMissing() {
        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);

        when(integrationInstance.getIntegrationInstanceConfigurationId()).thenReturn(5L);
        when(integrationInstanceService.getIntegrationInstance(9L)).thenReturn(integrationInstance);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);
        integrationInstanceConfiguration.setIntegrationId(7L);
        integrationInstanceConfiguration.setIntegrationVersion(3);

        when(integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(5L))
            .thenReturn(integrationInstanceConfiguration);
        when(integrationService.getIntegration(7L)).thenThrow(new NoSuchElementException("Integration 7 not found"));

        IntegrationJobPrincipalAccessor integrationJobPrincipalAccessor = new IntegrationJobPrincipalAccessor(
            integrationInstanceConfigurationService, mock(IntegrationInstanceConfigurationWorkflowService.class),
            integrationInstanceService, mock(IntegrationInstanceWorkflowService.class), integrationService,
            mock(IntegrationWorkflowService.class));

        assertThat(integrationJobPrincipalAccessor.getJobPrincipalSummary(9L))
            .isEqualTo(new JobPrincipalSummary(Environment.PRODUCTION.ordinal(), 7L, null, 3));
    }
}
