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

package com.bytechef.automation.ai.mcp.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.tag.service.TagService;
import org.junit.jupiter.api.Test;

class WorkspaceMcpServerFacadeImplTest {

    @Test
    void testUpdateWorkspaceMcpServerDelegatesToService() {
        McpServerService mcpServerService = mock(McpServerService.class);
        McpServer updated = mock(McpServer.class);

        when(mcpServerService.update(3L, "New name", true)).thenReturn(updated);

        WorkspaceMcpServerFacadeImpl facade = new WorkspaceMcpServerFacadeImpl(
            mock(McpProjectService.class), mock(McpServerFacade.class), mcpServerService,
            mock(TagService.class), mock(WorkspaceMcpServerService.class));

        McpServer result = facade.updateWorkspaceMcpServer(3L, "New name", true);

        assertThat(result).isSameAs(updated);

        verify(mcpServerService).update(3L, "New name", true);
    }
}
