/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectAdminFacade;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.web.graphql.config.EmbeddedConfigurationGraphQlTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    ConnectedUserProjectGraphQlController.class
})
@GraphQlTest(
    controllers = ConnectedUserProjectGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@EmbeddedConfigurationGraphQlConfigurationSharedMocks
@ExtendWith(ObjectMapperSetupExtension.class)
class ConnectedUserProjectGraphQlControllerIntTest {

    private static final String AUTOMATION_WORKFLOW_UUID = "4a1b2c3d-5e6f-4789-8a9b-0c1d2e3f4a5b";
    private static final String COPIED_FROM_WORKFLOW_UUID = "7d8e9f0a-1b2c-4d3e-8f4a-5b6c7d8e9f0a";

    @MockitoBean
    private ConnectedUserProjectAdminFacade connectedUserProjectAdminFacade;

    @MockitoBean
    private EnvironmentService environmentService;

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void testConnectedUserProjectsReturnsSourceWorkflowUuids() {
        when(environmentService.getEnvironment(0L)).thenReturn(Environment.DEVELOPMENT);
        when(connectedUserProjectAdminFacade.getConnectedUserProjects(7L, Environment.DEVELOPMENT))
            .thenReturn(List.of(createConnectedUserProjectDTO()));

        this.graphQlTester
            .document("""
                query {
                    connectedUserProjects(connectedUserId: "7", environmentId: "0") {
                        connectedUserProjectWorkflows {
                            id
                            automationWorkflowUuid
                            copiedFromWorkflowUuid
                        }
                    }
                }
                """)
            .execute()
            .path("connectedUserProjects[0].connectedUserProjectWorkflows[*].id")
            .entityList(String.class)
            .containsExactly("1", "2")
            .path("connectedUserProjects[0].connectedUserProjectWorkflows[0].automationWorkflowUuid")
            .entity(String.class)
            .isEqualTo(AUTOMATION_WORKFLOW_UUID)
            .path("connectedUserProjects[0].connectedUserProjectWorkflows[0].copiedFromWorkflowUuid")
            .valueIsNull()
            .path("connectedUserProjects[0].connectedUserProjectWorkflows[1].automationWorkflowUuid")
            .valueIsNull()
            .path("connectedUserProjects[0].connectedUserProjectWorkflows[1].copiedFromWorkflowUuid")
            .entity(String.class)
            .isEqualTo(COPIED_FROM_WORKFLOW_UUID);
    }

    private static ConnectedUserProjectDTO createConnectedUserProjectDTO() {
        ConnectedUserProjectWorkflowDTO referenceWorkflow = new ConnectedUserProjectWorkflowDTO(
            1L, 7L, true, null, 0L, null, AUTOMATION_WORKFLOW_UUID, 1, ConnectedUserProjectWorkflowDTO.Kind.REFERENCE,
            AUTOMATION_WORKFLOW_UUID, null, false, List.of(), List.of(), Map.of(), null, null);

        ConnectedUserProjectWorkflowDTO copyWorkflow = new ConnectedUserProjectWorkflowDTO(
            2L, 7L, true, null, 3L, null, "copy-workflow-uuid", 1, ConnectedUserProjectWorkflowDTO.Kind.COPY, null,
            COPIED_FROM_WORKFLOW_UUID, false, List.of(), List.of(), Map.of(), null, null);

        return new ConnectedUserProjectDTO(
            3L, null, List.of(referenceWorkflow, copyWorkflow), Environment.DEVELOPMENT, 0L, null, 3L, null, null,
            null, null, null, 0);
    }
}
