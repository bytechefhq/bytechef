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

package com.bytechef.automation.ai.tool;

import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import com.bytechef.automation.ai.mcp.facade.McpProjectWorkflowFacade;
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
public class McpServerToolCallbacksFactory {

    private final McpProjectFacade mcpProjectFacade;
    private final McpProjectWorkflowFacade mcpProjectWorkflowFacade;
    private final WorkspaceMcpServerFacade workspaceMcpServerFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public McpServerToolCallbacksFactory(
        McpProjectFacade mcpProjectFacade, McpProjectWorkflowFacade mcpProjectWorkflowFacade,
        WorkspaceMcpServerFacade workspaceMcpServerFacade) {

        this.mcpProjectFacade = mcpProjectFacade;
        this.mcpProjectWorkflowFacade = mcpProjectWorkflowFacade;
        this.workspaceMcpServerFacade = workspaceMcpServerFacade;
    }

    public List<ToolCallback> readToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        toolCallbacks.add(new ListMcpServersToolCallback(workspaceMcpServerFacade));
        toolCallbacks.add(new ListMcpProjectWorkflowsToolCallback(mcpProjectWorkflowFacade));

        return toolCallbacks;
    }

    public List<ToolCallback> writeToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>(readToolCallbacks());

        toolCallbacks.add(new CreateMcpServerToolCallback(workspaceMcpServerFacade));
        toolCallbacks.add(new UpdateMcpServerToolCallback(workspaceMcpServerFacade));
        toolCallbacks.add(new CreateMcpProjectToolCallback(mcpProjectFacade));
        toolCallbacks.add(new CloneMcpProjectToolCallback(mcpProjectFacade));
        toolCallbacks.add(new UpdateMcpProjectWorkflowParametersToolCallback(mcpProjectWorkflowFacade));

        return toolCallbacks;
    }
}
