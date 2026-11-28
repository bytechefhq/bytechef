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

package com.bytechef.automation.knowledgebase.tool.config;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.WorkspaceScopeResolver;
import com.bytechef.automation.ai.tool.WorkspaceScopedFlatToolCallback;
import com.bytechef.automation.knowledgebase.tool.knowledgebase.KnowledgeBaseToolCallbacksFactory;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class KnowledgeBaseMcpContributorConfiguration {

    @Bean
    McpServerToolCallbackContributor knowledgeBaseFlatCrudMcpContributor(
        ObjectProvider<KnowledgeBaseToolCallbacksFactory> knowledgeBaseToolCallbacksFactoryProvider,
        WorkspaceScopeResolver workspaceScopeResolver) {

        return () -> {
            KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory =
                knowledgeBaseToolCallbacksFactoryProvider
                    .getIfAvailable();

            if (knowledgeBaseToolCallbacksFactory == null) {
                return List.of();
            }

            List<ToolCallback> toolCallbacks = new ArrayList<>();

            for (ToolCallback toolCallback : knowledgeBaseToolCallbacksFactory.writeToolCallbacks()) {
                toolCallbacks.add(new WorkspaceScopedFlatToolCallback(toolCallback, workspaceScopeResolver));
            }

            return toolCallbacks;
        };
    }
}
