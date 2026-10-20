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

package com.bytechef.automation.ai.tool.config;

import com.bytechef.ai.copilot.tool.ask.SubAgentQuestionRenderer;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.platform.ai.skill.tool.ReadSkillsTools;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class CopilotAgentMcpContributorConfiguration {

    public static final Set<String> INTELLIGENT_TOOL_NAMES = Set.of(
        "buildWorkflow", "importWorkflow", "authorSkill", "debugWorkflowExecution", "configureMcpServer");

    @Bean
    McpServerToolCallbackContributor copilotAgentToolCallbackContributor(
        ObjectProvider<ReadSkillsTools> readSkillsToolsProvider, IntelligentToolCatalog intelligentToolCatalog,
        WorkspaceFacade workspaceFacade) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            readSkillsToolsProvider.ifAvailable(
                readSkillsTools -> toolCallbacks.addAll(List.of(ToolCallbacks.from(readSkillsTools))));

            toolCallbacks.addAll(
                intelligentToolCatalog.getByNames(
                    INTELLIGENT_TOOL_NAMES, IntelligentToolVariant.BUILD, (chatClient, definition) -> chatClient,
                    (toolCallback, definition) -> new WorkspaceScopedSubAgentToolCallback(toolCallback,
                        workspaceFacade),
                    SubAgentQuestionRenderer.PLAIN_TEXT));

            return toolCallbacks;
        };
    }
}
