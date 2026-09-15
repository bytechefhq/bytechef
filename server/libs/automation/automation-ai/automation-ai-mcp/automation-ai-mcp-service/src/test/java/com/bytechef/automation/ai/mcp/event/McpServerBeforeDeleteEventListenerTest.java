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

package com.bytechef.automation.ai.mcp.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.platform.mcp.domain.McpServer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.conversion.AggregateChange;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;

/**
 * @author Ivica Cardic
 */
class McpServerBeforeDeleteEventListenerTest {

    private static final long MCP_SERVER_ID = 5L;
    private static final long PROJECT_DEPLOYMENT_ID = 21L;

    @Test
    void testDeletesTheServersDeploymentsWithChecksSkipped() {
        McpProjectService mcpProjectService = mock(McpProjectService.class);
        ProjectDeploymentFacade projectDeploymentFacade = mock(ProjectDeploymentFacade.class);

        when(mcpProjectService.getMcpServerMcpProjects(MCP_SERVER_ID))
            .thenReturn(List.of(new McpProject(1L, PROJECT_DEPLOYMENT_ID, MCP_SERVER_ID)));

        List<Boolean> skipChecksDuringDelete = new ArrayList<>();

        doAnswer(invocation -> {
            skipChecksDuringDelete.add(AutomationAuthorizationContext.isSkipChecks());

            return null;
        }).when(projectDeploymentFacade)
            .deleteProjectDeployment(PROJECT_DEPLOYMENT_ID);

        McpServerBeforeDeleteEventListener mcpServerBeforeDeleteEventListener =
            new McpServerBeforeDeleteEventListener(mcpProjectService, projectDeploymentFacade);

        @SuppressWarnings("unchecked")
        AggregateChange<McpServer> aggregateChange = mock(AggregateChange.class);

        mcpServerBeforeDeleteEventListener.onBeforeDelete(
            new BeforeDeleteEvent<>(Identifier.of(MCP_SERVER_ID), new McpServer(), aggregateChange));

        // Deleting a deployment needs DEPLOYMENT_DELETE; a caller allowed to delete the server must not be stopped half
        // way by it.
        assertThat(skipChecksDuringDelete).containsExactly(true);
        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }
}
