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

import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class McpComponentOwnershipResolverTest {

    private final McpComponentRepository mcpComponentRepository = mock(McpComponentRepository.class);
    private final WorkspaceMcpServerService workspaceMcpServerService = mock(WorkspaceMcpServerService.class);
    private final McpComponentOwnershipResolver resolver = new McpComponentOwnershipResolver(
        mcpComponentRepository, workspaceMcpServerService);

    @Test
    void testResourceType() {
        assertThat(resolver.resourceType()).isEqualTo("McpComponent");
    }

    @Test
    void testResolvesWorkspaceViaServer() {
        McpComponent mcpComponent = mock(McpComponent.class);

        when(mcpComponent.getMcpServerId()).thenReturn(7L);
        when(mcpComponentRepository.findById(1L)).thenReturn(Optional.of(mcpComponent));
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(7L)).thenReturn(Optional.of(42L));

        assertThat(resolver.resolveOwner(1L)
            .workspaceId()).hasValue(42L);
    }

    @Test
    void testUnknownIsUnknown() {
        when(mcpComponentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(99L)
            .workspaceId()).isEmpty();
    }
}
