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

package com.bytechef.platform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.workflow.connection.ComponentConnectionFactoryResolver;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class ComponentConnectionFacadeTest {

    private static final String NESTED_WORKFLOW_DEFINITION = """
        {
          "label": "Nested",
          "triggers": [
            {"name": "newMessage1", "type": "slack/v1/newMessage"}
          ],
          "tasks": [
            {
              "name": "condition1",
              "type": "condition/v1",
              "parameters": {
                "rawExpression": true,
                "caseTrue": [
                  {"name": "createIssue1", "type": "jira/v1/createIssue", "parameters": {"summary": "s"}}
                ],
                "caseFalse": []
              }
            }
          ]
        }
        """;

    @Mock
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private ComponentConnectionFactoryResolver componentConnectionFactoryResolver;

    @Mock
    private WorkflowService workflowService;

    private ComponentConnectionFacadeImpl componentConnectionFacade;

    @BeforeEach
    void setUp() {
        componentConnectionFacade = new ComponentConnectionFacadeImpl(
            clusterElementDefinitionService, List.of(), componentDefinitionService,
            componentConnectionFactoryResolver, workflowService);
    }

    /**
     * The slot walk moved here from the embedded {@code WorkflowConnectionSlots}: triggers first, then every task
     * including those nested inside task dispatchers.
     */
    @Test
    void testGetWorkflowComponentConnectionsCoversTriggersAndTasksNestedInsideDispatchers() {
        ComponentConnectionFacadeImpl spiedComponentConnectionFacade = spy(componentConnectionFacade);

        when(workflowService.getWorkflow("wf-1"))
            .thenReturn(new Workflow("wf-1", NESTED_WORKFLOW_DEFINITION, Workflow.Format.JSON));

        doAnswer(invocation -> {
            WorkflowTrigger workflowTrigger = invocation.getArgument(0);

            return List.of(new ComponentConnection("slack", 1, workflowTrigger.getName(), "slack", true));
        }).when(spiedComponentConnectionFacade)
            .getComponentConnections(any(WorkflowTrigger.class));

        doAnswer(invocation -> {
            WorkflowTask workflowTask = invocation.getArgument(0);

            return "createIssue1".equals(workflowTask.getName())
                ? List.of(new ComponentConnection("jira", 1, "createIssue1", "jira", true))
                : List.of();
        }).when(spiedComponentConnectionFacade)
            .getComponentConnections(any(WorkflowTask.class));

        assertThat(spiedComponentConnectionFacade.getWorkflowComponentConnections("wf-1"))
            .extracting(ComponentConnection::componentName, ComponentConnection::workflowNodeName)
            .containsExactly(tuple("slack", "newMessage1"), tuple("jira", "createIssue1"));
    }
}
