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

package com.bytechef.automation.ai.mcp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.repository.McpProjectRepository;
import com.bytechef.automation.ai.mcp.repository.McpProjectWorkflowRepository;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class McpProjectWorkflowOwnershipResolverTest {

    private final McpProjectRepository mcpProjectRepository = mock(McpProjectRepository.class);
    private final McpProjectWorkflowRepository mcpProjectWorkflowRepository = mock(McpProjectWorkflowRepository.class);
    private final WorkspaceMcpServerService workspaceMcpServerService = mock(WorkspaceMcpServerService.class);
    private final McpProjectWorkflowOwnershipResolver resolver = new McpProjectWorkflowOwnershipResolver(
        mcpProjectRepository, mcpProjectWorkflowRepository, workspaceMcpServerService);

    @Test
    void testResourceType() {
        assertThat(resolver.resourceType()).isEqualTo("McpProjectWorkflow");
    }

    @Test
    void testResolvesWorkspaceViaProjectAndServer() {
        McpProjectWorkflow mcpProjectWorkflow = mock(McpProjectWorkflow.class);

        when(mcpProjectWorkflow.getMcpProjectId()).thenReturn(5L);
        when(mcpProjectWorkflowRepository.findById(1L)).thenReturn(Optional.of(mcpProjectWorkflow));

        McpProject mcpProject = mock(McpProject.class);

        when(mcpProject.getMcpServerId()).thenReturn(7L);
        when(mcpProjectRepository.findById(5L)).thenReturn(Optional.of(mcpProject));
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(7L)).thenReturn(Optional.of(42L));

        assertThat(resolver.resolveOwner(1L)
            .workspaceId()).hasValue(42L);
    }

    @Test
    void testUnknownIsUnknown() {
        when(mcpProjectWorkflowRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(99L)
            .workspaceId()).isEmpty();
    }
}
