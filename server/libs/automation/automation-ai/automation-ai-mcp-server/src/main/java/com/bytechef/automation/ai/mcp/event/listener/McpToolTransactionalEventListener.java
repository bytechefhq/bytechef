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

package com.bytechef.automation.ai.mcp.event.listener;

import com.bytechef.automation.execution.dto.ToolDTO;
import com.bytechef.automation.execution.facade.ToolFacade;
import com.bytechef.platform.mcp.domain.McpTool;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.data.relational.core.mapping.event.AfterDeleteEvent;
import org.springframework.data.relational.core.mapping.event.AfterSaveEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * @author Matija Petanjek
 */
@Component
public class McpToolTransactionalEventListener {

    private final McpAsyncServer mcpSyncServer;
    private final ToolFacade toolFacade;

    @SuppressFBWarnings("EI")
    public McpToolTransactionalEventListener(McpAsyncServer mcpSyncServer, ToolFacade toolFacade) {
        this.mcpSyncServer = mcpSyncServer;
        this.toolFacade = toolFacade;
    }

    @TransactionalEventListener
    public void handleEvent(AfterSaveEvent<McpTool> mcpToolSaveEvent) {
        mcpSyncServer.addTool(
            McpToolUtils.toAsyncToolSpecification(
                toolFacade.getFunctionToolCallback(
                    toolFacade.toToolDTO(mcpToolSaveEvent.getEntity()))));
        mcpSyncServer.notifyToolsListChanged();
    }

    @TransactionalEventListener
    public void handleEvent(AfterDeleteEvent<McpTool> mcpToolDeleteEvent) {
        ToolDTO toolDTO = toolFacade.toToolDTO(mcpToolDeleteEvent.getEntity());

        mcpSyncServer.removeTool(toolDTO.name());

        mcpSyncServer.notifyToolsListChanged();
    }
}
