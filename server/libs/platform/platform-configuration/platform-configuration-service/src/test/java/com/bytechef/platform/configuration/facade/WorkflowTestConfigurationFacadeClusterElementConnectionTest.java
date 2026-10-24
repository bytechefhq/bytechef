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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowTestConfigurationFacadeClusterElementConnectionTest {

    private static final Workflow AGENT_WORKFLOW = new Workflow(
        "workflow1",
        """
            {
                "tasks": [
                    {
                        "name": "aiAgent_1",
                        "type": "aiAgent/v1/chat",
                        "parameters": {},
                        "clusterElements": {
                            "model": {"name": "model_1", "type": "anthropic/v1/model", "parameters": {}}
                        }
                    },
                    {
                        "name": "sendChannelMessage_1",
                        "type": "slack/v1/sendChannelMessage",
                        "parameters": {}
                    }
                ]
            }
            """,
        Format.JSON);

    private final ComponentConnectionFacade componentConnectionFacade = mock(ComponentConnectionFacade.class);
    private final ConnectionService connectionService = mock(ConnectionService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final WorkflowTestConfigurationService workflowTestConfigurationService =
        mock(WorkflowTestConfigurationService.class);
    private final WorkflowTestConfigurationFacade workflowTestConfigurationFacade =
        new WorkflowTestConfigurationFacadeImpl(
            connectionService, componentConnectionFacade, workflowService, workflowTestConfigurationService);

    @BeforeEach
    void beforeEach() {
        when(workflowService.getWorkflow("workflow1")).thenReturn(AGENT_WORKFLOW);
        when(componentConnectionFacade.getComponentConnections(any(WorkflowTask.class)))
            .thenAnswer(invocation -> {
                WorkflowTask workflowTask = invocation.getArgument(0);

                return "aiAgent_1".equals(workflowTask.getName())
                    ? List.of(new ComponentConnection("anthropic", 1, "aiAgent_1", "model_1", true))
                    : List.of(new ComponentConnection("slack", 1, "sendChannelMessage_1", "slack", true));
            });
    }

    @Test
    void testSaveBindsAClusterElementUnderItsRootTaskWithItsNameAsTheKey() {
        stubConnection(7L, "anthropic");

        when(componentConnectionFacade.getComponentConnection("workflow1", "aiAgent_1", "model_1"))
            .thenReturn(new ComponentConnection("anthropic", 1, "aiAgent_1", "model_1", true));

        workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            "workflow1", "model_1", "anthropic", 7L, 0L);

        verify(workflowTestConfigurationService).saveWorkflowTestConfigurationConnection(
            "workflow1", "aiAgent_1", "model_1", 7L, false, 0L);
    }

    @Test
    void testSaveKeepsATaskNameAndItsKey() {
        stubConnection(8L, "slack");

        when(componentConnectionFacade.getComponentConnection("workflow1", "sendChannelMessage_1", "slack"))
            .thenReturn(new ComponentConnection("slack", 1, "sendChannelMessage_1", "slack", true));

        workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            "workflow1", "sendChannelMessage_1", "slack", 8L, 0L);

        verify(workflowTestConfigurationService).saveWorkflowTestConfigurationConnection(
            "workflow1", "sendChannelMessage_1", "slack", 8L, false, 0L);
    }

    private void stubConnection(long connectionId, String componentName) {
        Connection connection = new Connection();

        connection.setComponentName(componentName);

        when(connectionService.getConnection(connectionId)).thenReturn(connection);
    }
}
