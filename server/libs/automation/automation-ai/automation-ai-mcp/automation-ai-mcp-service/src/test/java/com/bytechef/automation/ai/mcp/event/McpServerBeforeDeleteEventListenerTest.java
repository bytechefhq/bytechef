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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.platform.mcp.domain.McpServer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;

/**
 * @author Ivica Cardic
 */
class McpServerBeforeDeleteEventListenerTest {

    private final McpProjectService mcpProjectService = mock(McpProjectService.class);
    private final ProjectDeploymentFacade projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
    private final McpServerBeforeDeleteEventListener mcpServerBeforeDeleteEventListener =
        new McpServerBeforeDeleteEventListener(mcpProjectService, projectDeploymentFacade);

    @Test
    void testOnBeforeDeleteDeletesEverySystemDeploymentThroughTheDeploymentFacade() {
        McpProject mcpProject1 = new McpProject(100L, 1L);

        mcpProject1.setId(10L);

        McpProject mcpProject2 = new McpProject(200L, 1L);

        mcpProject2.setId(20L);

        when(mcpProjectService.getMcpServerMcpProjects(1L)).thenReturn(List.of(mcpProject1, mcpProject2));

        mcpServerBeforeDeleteEventListener.onBeforeDelete(beforeDeleteEvent(1L));

        verify(projectDeploymentFacade).deleteProjectDeployment(100L);
        verify(projectDeploymentFacade).deleteProjectDeployment(200L);
    }

    @Test
    void testOnBeforeDeleteWithoutMcpProjectsDeletesNothing() {
        when(mcpProjectService.getMcpServerMcpProjects(1L)).thenReturn(List.of());

        mcpServerBeforeDeleteEventListener.onBeforeDelete(beforeDeleteEvent(1L));

        verifyNoInteractions(projectDeploymentFacade);
    }

    @SuppressWarnings("unchecked")
    private static BeforeDeleteEvent<McpServer> beforeDeleteEvent(long mcpServerId) {
        BeforeDeleteEvent<McpServer> beforeDeleteEvent = mock(BeforeDeleteEvent.class);
        Identifier identifier = mock(Identifier.class);

        when(beforeDeleteEvent.getId()).thenReturn(identifier);
        when(identifier.getValue()).thenReturn(mcpServerId);

        return beforeDeleteEvent;
    }
}
