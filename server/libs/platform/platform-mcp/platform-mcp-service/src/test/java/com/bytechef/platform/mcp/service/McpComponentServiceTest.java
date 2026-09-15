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

package com.bytechef.platform.mcp.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * @author Ivica Cardic
 */
class McpComponentServiceTest {

    private static final long CONNECTION_ID = 8L;
    private static final long MCP_SERVER_ID = 5L;

    private final McpComponentConnectionUsageChecker mcpComponentConnectionUsageChecker =
        mock(McpComponentConnectionUsageChecker.class);
    private final McpComponentRepository mcpComponentRepository = mock(McpComponentRepository.class);

    private final McpComponentServiceImpl mcpComponentService = new McpComponentServiceImpl(
        mcpComponentRepository, List.of(mcpComponentConnectionUsageChecker));

    @Test
    void testCreateChecksTheConnectionAgainstTheServer() {
        mcpComponentService.create(new McpComponent("slack", 1, MCP_SERVER_ID, CONNECTION_ID));

        verify(mcpComponentConnectionUsageChecker).checkConnectionUsage(MCP_SERVER_ID, CONNECTION_ID);
        verify(mcpComponentRepository).save(any(McpComponent.class));
    }

    @Test
    void testCreateRefusesAConnectionTheCheckerRefuses() {
        doThrow(new AccessDeniedException("other workspace"))
            .when(mcpComponentConnectionUsageChecker)
            .checkConnectionUsage(MCP_SERVER_ID, CONNECTION_ID);

        assertThatThrownBy(
            () -> mcpComponentService.create(new McpComponent("slack", 1, MCP_SERVER_ID, CONNECTION_ID)))
                .isInstanceOf(AccessDeniedException.class);

        verify(mcpComponentRepository, never()).save(any());
    }

    @Test
    void testCreateWithoutAConnectionSkipsTheCheck() {
        mcpComponentService.create(new McpComponent("httpClient", 1, MCP_SERVER_ID, null));

        verify(mcpComponentConnectionUsageChecker, never()).checkConnectionUsage(anyLong(), anyLong());
    }

    @Test
    void testUpdateChecksTheNewConnectionAgainstTheStoredServer() {
        McpComponent storedMcpComponent = new McpComponent("slack", 1, MCP_SERVER_ID, null);

        storedMcpComponent.setId(4L);

        when(mcpComponentRepository.findById(4L)).thenReturn(Optional.of(storedMcpComponent));

        doThrow(new AccessDeniedException("other workspace"))
            .when(mcpComponentConnectionUsageChecker)
            .checkConnectionUsage(MCP_SERVER_ID, CONNECTION_ID);

        McpComponent mcpComponent = new McpComponent("slack", 1, MCP_SERVER_ID + 1, CONNECTION_ID);

        mcpComponent.setId(4L);

        assertThatThrownBy(() -> mcpComponentService.update(mcpComponent))
            .isInstanceOf(AccessDeniedException.class);

        verify(mcpComponentRepository, never()).save(any());
    }
}
