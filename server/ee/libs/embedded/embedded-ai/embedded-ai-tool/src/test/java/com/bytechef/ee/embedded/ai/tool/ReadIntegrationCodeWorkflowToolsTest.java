/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.ai.tool.exception.IntegrationCodeWorkflowToolErrorType;
import com.bytechef.ee.embedded.ai.tool.model.IntegrationCodeWorkflowInfo;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.facade.IntegrationCodeWorkflowFacade;
import com.bytechef.exception.ExecutionException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ReadIntegrationCodeWorkflowToolsTest {

    @Mock
    private IntegrationCodeWorkflowFacade integrationCodeWorkflowFacade;

    @Test
    void testGetIntegrationCodeWorkflowSourceReturnsSourceText() {
        ReadIntegrationCodeWorkflowTools tools = new ReadIntegrationCodeWorkflowTools(integrationCodeWorkflowFacade);

        when(integrationCodeWorkflowFacade.getCodeWorkflowSource(3L)).thenReturn("({name: 'my-integration'})");

        String result = tools.getIntegrationCodeWorkflowSource(3L);

        verify(integrationCodeWorkflowFacade).getCodeWorkflowSource(3L);
        assertThat(result).isEqualTo("({name: 'my-integration'})");
    }

    @Test
    void testGetIntegrationCodeWorkflowSourceThrowsExecutionExceptionOnFacadeFailure() {
        ReadIntegrationCodeWorkflowTools tools = new ReadIntegrationCodeWorkflowTools(integrationCodeWorkflowFacade);

        when(integrationCodeWorkflowFacade.getCodeWorkflowSource(3L)).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> tools.getIntegrationCodeWorkflowSource(3L))
            .isInstanceOf(ExecutionException.class)
            .extracting(thrown -> ((ExecutionException) thrown).getErrorKey())
            .isEqualTo(IntegrationCodeWorkflowToolErrorType.GET_SOURCE.getErrorKey());
    }

    @Test
    void testListIntegrationCodeWorkflowsReturnsFacadeListWithResolvedLanguage() {
        ReadIntegrationCodeWorkflowTools tools = new ReadIntegrationCodeWorkflowTools(integrationCodeWorkflowFacade);

        Integration integration = new Integration();

        integration.setId(1L);
        integration.setComponentName("my-integration");

        when(integrationCodeWorkflowFacade.getCodeWorkflowIntegrations()).thenReturn(List.of(integration));
        when(integrationCodeWorkflowFacade.getCodeWorkflowLanguage(1L))
            .thenReturn(Optional.of("JAVASCRIPT"));

        List<IntegrationCodeWorkflowInfo> result = tools.listIntegrationCodeWorkflows();

        verify(integrationCodeWorkflowFacade).getCodeWorkflowIntegrations();
        assertThat(result).containsExactly(
            new IntegrationCodeWorkflowInfo(1L, "my-integration", "JAVASCRIPT"));
    }

    @Test
    void testListIntegrationCodeWorkflowsReturnsNullLanguageWhenInfoUnavailable() {
        ReadIntegrationCodeWorkflowTools tools = new ReadIntegrationCodeWorkflowTools(integrationCodeWorkflowFacade);

        Integration integration = new Integration();

        integration.setId(1L);
        integration.setComponentName("my-integration");

        when(integrationCodeWorkflowFacade.getCodeWorkflowIntegrations()).thenReturn(List.of(integration));
        when(integrationCodeWorkflowFacade.getCodeWorkflowLanguage(1L)).thenReturn(Optional.empty());

        List<IntegrationCodeWorkflowInfo> result = tools.listIntegrationCodeWorkflows();

        assertThat(result).containsExactly(
            new IntegrationCodeWorkflowInfo(1L, "my-integration", null));
    }

    @Test
    void testListIntegrationCodeWorkflowsThrowsExecutionExceptionOnFacadeFailure() {
        ReadIntegrationCodeWorkflowTools tools = new ReadIntegrationCodeWorkflowTools(integrationCodeWorkflowFacade);

        when(integrationCodeWorkflowFacade.getCodeWorkflowIntegrations()).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(tools::listIntegrationCodeWorkflows)
            .isInstanceOf(ExecutionException.class)
            .extracting(thrown -> ((ExecutionException) thrown).getErrorKey())
            .isEqualTo(IntegrationCodeWorkflowToolErrorType.LIST.getErrorKey());
    }
}
