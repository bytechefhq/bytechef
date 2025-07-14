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

package com.bytechef.mcp.server;

import com.bytechef.mcp.tool.ProjectTools;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "bytechef.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
public class McpServerConfiguration {

    private final ProjectTools projectTools;

    public McpServerConfiguration(ProjectTools projectTools) {
        this.projectTools = projectTools;
    }

    @Bean
    ToolCallbackProvider toolCallbackProvider() {
        return ToolCallbackProvider.from(ToolCallbacks.from(projectTools));
    }
}
