/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.dto.IntegrationConnectionWorkflowDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectionFacade;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlTestConfiguration;
import java.util.List;
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
    IntegrationConnectionWorkflowGraphQlController.class
})
@GraphQlTest(
    controllers = IntegrationConnectionWorkflowGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@EmbeddedConfigurationGraphQlConfigurationSharedMocks
class IntegrationConnectionWorkflowGraphQlControllerIntTest {

    @MockitoBean(name = "com.bytechef.ee.embedded.configuration.facade.ConnectionFacade")
    private ConnectionFacade connectionFacade;

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void testIntegrationConnectionWorkflows() {
        when(connectionFacade.getConnectionWorkflows(321L))
            .thenReturn(
                List.of(
                    new IntegrationConnectionWorkflowDTO(
                        7L, 11L, "HubSpot Sync", IntegrationConnectionWorkflowDTO.ParentType.INTEGRATION, 2,
                        IntegrationConnectionWorkflowDTO.Usage.DEPLOYMENT, "workflow-1", "Push contacts", "uuid-1"),
                    new IntegrationConnectionWorkflowDTO(
                        8L, 1050L, "jane@example.com", IntegrationConnectionWorkflowDTO.ParentType.CONNECTED_USER, 4,
                        IntegrationConnectionWorkflowDTO.Usage.EDITOR, "workflow-2", "Route invoice emails", null)));

        graphQlTester
            .document("""
                query {
                    integrationConnectionWorkflows(connectionId: 321) {
                        id
                        parentId
                        parentName
                        parentType
                        parentVersion
                        usage
                        workflowId
                        workflowName
                        workflowUuid
                    }
                }
                """)
            .execute()
            .path("integrationConnectionWorkflows[0].id")
            .entity(String.class)
            .isEqualTo("7")
            .path("integrationConnectionWorkflows[0].parentName")
            .entity(String.class)
            .isEqualTo("HubSpot Sync")
            .path("integrationConnectionWorkflows[0].parentType")
            .entity(String.class)
            .isEqualTo("INTEGRATION")
            .path("integrationConnectionWorkflows[0].parentVersion")
            .entity(Integer.class)
            .isEqualTo(2)
            .path("integrationConnectionWorkflows[0].usage")
            .entity(String.class)
            .isEqualTo("DEPLOYMENT")
            .path("integrationConnectionWorkflows[0].workflowUuid")
            .entity(String.class)
            .isEqualTo("uuid-1")
            .path("integrationConnectionWorkflows[1].parentId")
            .entity(String.class)
            .isEqualTo("1050")
            .path("integrationConnectionWorkflows[1].parentType")
            .entity(String.class)
            .isEqualTo("CONNECTED_USER")
            .path("integrationConnectionWorkflows[1].usage")
            .entity(String.class)
            .isEqualTo("EDITOR")
            .path("integrationConnectionWorkflows[1].workflowUuid")
            .valueIsNull();
    }
}
