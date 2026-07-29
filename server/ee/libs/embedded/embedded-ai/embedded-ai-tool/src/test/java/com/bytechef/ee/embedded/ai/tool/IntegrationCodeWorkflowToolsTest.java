/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.ai.tool.exception.IntegrationCodeWorkflowToolErrorType;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.facade.IntegrationCodeWorkflowFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.exception.ExecutionException;
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
class IntegrationCodeWorkflowToolsTest {

    @Mock
    private IntegrationCodeWorkflowFacade integrationCodeWorkflowFacade;

    @Test
    void testCreateIntegrationCodeWorkflowReturnsConfirmationMessage() {
        IntegrationCodeWorkflowTools tools = new IntegrationCodeWorkflowTools(integrationCodeWorkflowFacade);

        Integration integration = new Integration();

        integration.setId(7L);
        integration.setComponentName("my-integration");

        when(integrationCodeWorkflowFacade.createEmptyCodeWorkflow("my-integration", Language.JAVASCRIPT))
            .thenReturn(integration);

        String result = tools.createIntegrationCodeWorkflow("my-integration", "JAVASCRIPT");

        verify(integrationCodeWorkflowFacade).createEmptyCodeWorkflow("my-integration", Language.JAVASCRIPT);
        assertThat(result).contains("7")
            .contains("my-integration");
    }

    @Test
    void testCreateIntegrationCodeWorkflowAcceptsLanguageNameCaseInsensitively() {
        IntegrationCodeWorkflowTools tools = new IntegrationCodeWorkflowTools(integrationCodeWorkflowFacade);

        Integration integration = new Integration();

        integration.setId(3L);
        integration.setComponentName("my-integration");

        when(integrationCodeWorkflowFacade.createEmptyCodeWorkflow("my-integration", Language.PYTHON))
            .thenReturn(integration);

        tools.createIntegrationCodeWorkflow("my-integration", "python");

        verify(integrationCodeWorkflowFacade).createEmptyCodeWorkflow("my-integration", Language.PYTHON);
    }

    @Test
    void testCreateIntegrationCodeWorkflowThrowsExecutionExceptionOnFacadeFailure() {
        IntegrationCodeWorkflowTools tools = new IntegrationCodeWorkflowTools(integrationCodeWorkflowFacade);

        when(integrationCodeWorkflowFacade.createEmptyCodeWorkflow(anyString(), any()))
            .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> tools.createIntegrationCodeWorkflow("my-integration", "JAVASCRIPT"))
            .isInstanceOf(ExecutionException.class)
            .extracting(thrown -> ((ExecutionException) thrown).getErrorKey())
            .isEqualTo(IntegrationCodeWorkflowToolErrorType.CREATE.getErrorKey());
    }

    @Test
    void testCreateIntegrationCodeWorkflowRejectsJavaLanguage() {
        IntegrationCodeWorkflowTools tools = new IntegrationCodeWorkflowTools(integrationCodeWorkflowFacade);

        assertThatThrownBy(() -> tools.createIntegrationCodeWorkflow("my-integration", "JAVA"))
            .isInstanceOf(ExecutionException.class)
            .extracting(thrown -> ((ExecutionException) thrown).getErrorKey())
            .isEqualTo(IntegrationCodeWorkflowToolErrorType.UNSUPPORTED_LANGUAGE.getErrorKey());

        verifyNoInteractions(integrationCodeWorkflowFacade);
    }

    @Test
    void testCreateIntegrationCodeWorkflowRejectsUnknownLanguage() {
        IntegrationCodeWorkflowTools tools = new IntegrationCodeWorkflowTools(integrationCodeWorkflowFacade);

        assertThatThrownBy(() -> tools.createIntegrationCodeWorkflow("my-integration", "COBOL"))
            .isInstanceOf(ExecutionException.class)
            .extracting(thrown -> ((ExecutionException) thrown).getErrorKey())
            .isEqualTo(IntegrationCodeWorkflowToolErrorType.UNSUPPORTED_LANGUAGE.getErrorKey());

        verifyNoInteractions(integrationCodeWorkflowFacade);
    }

    @Test
    void testCreateIntegrationCodeWorkflowRejectsBlankLanguage() {
        IntegrationCodeWorkflowTools tools = new IntegrationCodeWorkflowTools(integrationCodeWorkflowFacade);

        assertThatThrownBy(() -> tools.createIntegrationCodeWorkflow("my-integration", "   "))
            .isInstanceOf(ExecutionException.class)
            .extracting(thrown -> ((ExecutionException) thrown).getErrorKey())
            .isEqualTo(IntegrationCodeWorkflowToolErrorType.UNSUPPORTED_LANGUAGE.getErrorKey());

        verifyNoInteractions(integrationCodeWorkflowFacade);
    }

    @Test
    void testUpdateIntegrationCodeWorkflowSourceReturnsConfirmationMessage() {
        IntegrationCodeWorkflowTools tools = new IntegrationCodeWorkflowTools(integrationCodeWorkflowFacade);

        String result = tools.updateIntegrationCodeWorkflowSource(5L, "({name: 'my-integration'})");

        verify(integrationCodeWorkflowFacade).updateCodeWorkflowSource(5L, "({name: 'my-integration'})");
        assertThat(result).isNotBlank()
            .contains("5");
    }

    @Test
    void testUpdateIntegrationCodeWorkflowSourceThrowsExecutionExceptionOnFacadeFailure() {
        IntegrationCodeWorkflowTools tools = new IntegrationCodeWorkflowTools(integrationCodeWorkflowFacade);

        doThrow(new RuntimeException("boom"))
            .when(integrationCodeWorkflowFacade)
            .updateCodeWorkflowSource(5L, "content");

        assertThatThrownBy(() -> tools.updateIntegrationCodeWorkflowSource(5L, "content"))
            .isInstanceOf(ExecutionException.class)
            .extracting(thrown -> ((ExecutionException) thrown).getErrorKey())
            .isEqualTo(IntegrationCodeWorkflowToolErrorType.UPDATE_SOURCE.getErrorKey());
    }
}
