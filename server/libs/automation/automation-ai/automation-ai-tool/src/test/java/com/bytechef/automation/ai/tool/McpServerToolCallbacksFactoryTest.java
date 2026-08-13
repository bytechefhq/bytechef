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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
final class McpServerToolCallbacksFactoryTest {

    private final McpServerToolCallbacksFactory factory = new McpServerToolCallbacksFactory(
        mock(McpProjectFacade.class), mock(McpProjectService.class), mock(McpProjectWorkflowService.class),
        mock(ProjectDeploymentWorkflowService.class), mock(WorkflowService.class),
        mock(WorkspaceMcpServerFacade.class));

    @Test
    void readListContainsListMcpServersAndListMcpProjectWorkflows() {
        List<String> names = toolNames(factory.readToolCallbacks());

        assertThat(names).containsExactly("listMcpServers", "listMcpProjectWorkflows");
    }

    @Test
    void writeListIncludesReadsAndAllMutations() {
        List<String> names = toolNames(factory.writeToolCallbacks());

        assertThat(names).containsExactlyInAnyOrder(
            "listMcpServers", "listMcpProjectWorkflows", "createMcpServer", "updateMcpServer", "createMcpProject",
            "cloneMcpProject", "updateMcpProjectWorkflowParameters");
    }

    private static List<String> toolNames(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();
    }
}
