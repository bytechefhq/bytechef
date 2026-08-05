/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.facade.IntegrationCodeWorkflowFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationCodeWorkflowGraphQlControllerTest {

    private final IntegrationCodeWorkflowFacade integrationCodeWorkflowFacade =
        mock(IntegrationCodeWorkflowFacade.class);
    private final IntegrationCodeWorkflowGraphQlController controller =
        new IntegrationCodeWorkflowGraphQlController(integrationCodeWorkflowFacade);

    @Test
    void testIntegrationCodeWorkflowSourceDelegatesToFacade() {
        when(integrationCodeWorkflowFacade.getCodeWorkflowSource(42L)).thenReturn("console.log('hi');");

        String source = controller.integrationCodeWorkflowSource(42L);

        assertThat(source).isEqualTo("console.log('hi');");

        verify(integrationCodeWorkflowFacade).getCodeWorkflowSource(42L);
    }

    @Test
    void testUpdateIntegrationCodeWorkflowSourceDelegatesToFacadeAndReturnsTrue() {
        boolean result = controller.updateIntegrationCodeWorkflowSource(42L, "console.log('updated');");

        assertThat(result).isTrue();

        verify(integrationCodeWorkflowFacade).updateCodeWorkflowSource(42L, "console.log('updated');");
    }

    @Test
    void testCreateIntegrationCodeWorkflowDelegatesToFacadeAndReturnsIntegrationId() {
        Integration integration = new Integration();

        integration.setId(123L);
        integration.setComponentName("my-code-component");

        when(integrationCodeWorkflowFacade.createEmptyCodeWorkflow(
            eq("my-code-component"), eq(Language.JAVASCRIPT), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(integration);

        String integrationId = controller.createIntegrationCodeWorkflow("my-code-component", Language.JAVASCRIPT, null,
            null, null, null, null);

        assertThat(integrationId).isEqualTo("123");

        verify(integrationCodeWorkflowFacade).createEmptyCodeWorkflow("my-code-component", Language.JAVASCRIPT, null,
            null, null, null, null);
    }

    @Test
    void testCreateIntegrationCodeWorkflowSupportsAllNonJavaLanguages() {
        Integration integration = new Integration();

        integration.setId(1L);

        when(integrationCodeWorkflowFacade.createEmptyCodeWorkflow(anyString(), eq(Language.PYTHON), isNull(), isNull(),
            isNull(), isNull(), isNull()))
                .thenReturn(integration);
        when(integrationCodeWorkflowFacade.createEmptyCodeWorkflow(anyString(), eq(Language.RUBY), isNull(), isNull(),
            isNull(), isNull(), isNull()))
                .thenReturn(integration);

        assertThat(
            controller.createIntegrationCodeWorkflow("python-component", Language.PYTHON, null, null, null, null, null))
                .isEqualTo("1");
        assertThat(
            controller.createIntegrationCodeWorkflow("ruby-component", Language.RUBY, null, null, null, null, null))
                .isEqualTo("1");
    }
}
