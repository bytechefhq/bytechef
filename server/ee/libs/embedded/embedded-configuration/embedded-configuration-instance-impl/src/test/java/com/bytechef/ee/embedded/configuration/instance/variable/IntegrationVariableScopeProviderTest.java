/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.instance.variable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.platform.constant.PlatformType;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class IntegrationVariableScopeProviderTest {

    private final IntegrationWorkflowService integrationWorkflowService = mock(IntegrationWorkflowService.class);
    private final IntegrationVariableScopeProvider provider = new IntegrationVariableScopeProvider(
        integrationWorkflowService);

    @Test
    void testTypeIsEmbedded() {
        assertThat(provider.getType()).isEqualTo(PlatformType.EMBEDDED);
    }

    @Test
    void testJobPrincipalAlwaysMapsToEmbeddedScope() {
        assertThat(provider.getVariableScope(123L)).contains(VariableScope.embedded());
    }

    @Test
    void testWorkflowIdMapsToEmbeddedScopeOnlyWhenIntegrationWorkflowExists() {
        when(integrationWorkflowService.fetchWorkflowIntegrationWorkflow("wf-e")).thenReturn(
            Optional.of(mock(IntegrationWorkflow.class)));
        when(integrationWorkflowService.fetchWorkflowIntegrationWorkflow("wf-a")).thenReturn(Optional.empty());

        assertThat(provider.getVariableScopeByWorkflowId("wf-e")).contains(VariableScope.embedded());
        assertThat(provider.getVariableScopeByWorkflowId("wf-a")).isEmpty();
    }
}
