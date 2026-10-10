/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.copilot.web.graphql.facade.WorkflowDescriptionCopilotFacade;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotRequest;
import com.bytechef.ee.ai.copilot.workflow.WorkflowDescriptionCopilotResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = WorkflowDescriptionCopilotGraphQlController.class)
@GraphQlTest(
    controllers = WorkflowDescriptionCopilotGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
class WorkflowDescriptionCopilotGraphQlControllerIntTest {

    @MockitoBean
    private WorkflowDescriptionCopilotFacade workflowDescriptionCopilotFacade;

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void testGenerateWorkflowDescriptionDelegatesToTheGuardedFacade() {
        when(workflowDescriptionCopilotFacade.generateWorkflowDescription(any()))
            .thenReturn(new WorkflowDescriptionCopilotResult("Syncs records."));

        graphQlTester
            .document(
                """
                    mutation {
                        generateWorkflowDescription(input: {
                            workflowId: "wf1"
                            environmentId: 0
                        }) {
                            value
                        }
                    }
                    """)
            .execute()
            .path("generateWorkflowDescription.value")
            .entity(String.class)
            .isEqualTo("Syncs records.");

        ArgumentCaptor<WorkflowDescriptionCopilotRequest> requestArgumentCaptor =
            ArgumentCaptor.forClass(WorkflowDescriptionCopilotRequest.class);

        verify(workflowDescriptionCopilotFacade).generateWorkflowDescription(requestArgumentCaptor.capture());

        assertThat(requestArgumentCaptor.getValue())
            .isEqualTo(new WorkflowDescriptionCopilotRequest("wf1", null, 0));
    }
}
