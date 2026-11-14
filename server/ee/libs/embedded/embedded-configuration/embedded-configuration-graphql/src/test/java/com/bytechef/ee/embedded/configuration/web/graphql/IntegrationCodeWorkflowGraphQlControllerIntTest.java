/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.facade.IntegrationCodeWorkflowFacade;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlTestConfiguration;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    EmbeddedConfigurationGraphQlTestConfiguration.class,
    IntegrationCodeWorkflowGraphQlController.class
})
@GraphQlTest(
    controllers = IntegrationCodeWorkflowGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@EmbeddedConfigurationGraphQlConfigurationSharedMocks
class IntegrationCodeWorkflowGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private IntegrationCodeWorkflowFacade integrationCodeWorkflowFacade;

    @Test
    void testIntegrationCodeWorkflowSource() {
        when(integrationCodeWorkflowFacade.getCodeWorkflowSource(42L)).thenReturn("console.log('hi');");

        graphQlTester
            .document("query { integrationCodeWorkflowSource(integrationId: 42) }")
            .execute()
            .path("integrationCodeWorkflowSource")
            .entity(String.class)
            .isEqualTo("console.log('hi');");
    }

    @Test
    void testUpdateIntegrationCodeWorkflowSource() {
        graphQlTester
            .document(
                "mutation { updateIntegrationCodeWorkflowSource(integrationId: 42, content: \"console.log('updated');\") }")
            .execute()
            .path("updateIntegrationCodeWorkflowSource")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(integrationCodeWorkflowFacade).updateCodeWorkflowSource(42L, "console.log('updated');");
    }

    @Test
    void testCreateIntegrationCodeWorkflow() {
        Integration integration = new Integration();

        integration.setId(123L);

        when(integrationCodeWorkflowFacade.createEmptyCodeWorkflow(
            eq("my-code-component"), eq(Language.PYTHON), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(integration);

        graphQlTester
            .document(
                "mutation { createIntegrationCodeWorkflow(componentName: \"my-code-component\", language: PYTHON) }")
            .execute()
            .path("createIntegrationCodeWorkflow")
            .entity(String.class)
            .isEqualTo("123");
    }
}
