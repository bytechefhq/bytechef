/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.copilot.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.facade.WorkflowDescriptionCopilotFacade;
import com.bytechef.ai.copilot.workflow.WorkflowDescriptionCopilotRequest;
import com.bytechef.ai.copilot.workflow.WorkflowDescriptionCopilotResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = WorkflowDescriptionCopilotGraphQlController.class)
@GraphQlTest(
    controllers = WorkflowDescriptionCopilotGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
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
