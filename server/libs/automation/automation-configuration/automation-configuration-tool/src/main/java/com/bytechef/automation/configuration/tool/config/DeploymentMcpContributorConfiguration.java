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

package com.bytechef.automation.configuration.tool.config;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.WorkspaceScopedFlatToolCallback;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.tool.DeploymentToolCallbacksFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class DeploymentMcpContributorConfiguration {

    private static final Set<String> WORKSPACE_SCOPED_DEPLOYMENT_TOOL_NAMES = Set.of("listProjectDeployments");

    @Bean
    McpServerToolCallbackContributor deploymentFlatCrudMcpContributor(
        ObjectProvider<DeploymentToolCallbacksFactory> deploymentToolCallbacksFactoryProvider,
        WorkspaceFacade workspaceFacade) {

        return () -> {
            DeploymentToolCallbacksFactory deploymentToolCallbacksFactory = deploymentToolCallbacksFactoryProvider
                .getIfAvailable();

            if (deploymentToolCallbacksFactory == null) {
                return List.of();
            }

            List<ToolCallback> toolCallbacks = new ArrayList<>();

            for (ToolCallback toolCallback : deploymentToolCallbacksFactory.writeToolCallbacks()) {
                String name = toolCallback.getToolDefinition()
                    .name();

                toolCallbacks.add(
                    WORKSPACE_SCOPED_DEPLOYMENT_TOOL_NAMES.contains(name)
                        ? new WorkspaceScopedFlatToolCallback(toolCallback, workspaceFacade)
                        : toolCallback);
            }

            return toolCallbacks;
        };
    }
}
