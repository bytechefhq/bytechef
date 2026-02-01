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

package com.bytechef.ai.mcp.tool.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that matches when either MCP server or AI Copilot is enabled.
 *
 * <p>
 * This condition returns {@code true} if any of these conditions are met:
 * <ul>
 * <li>{@code bytechef.ai.mcp.server.enabled} is {@code true}</li>
 * <li>{@code bytechef.ai.copilot.enabled} is {@code true}</li>
 * </ul>
 *
 * @author Ivica Cardic
 */
public class OnAiEnabledCondition implements Condition {

    private static final String MCP_SERVER_ENABLED_PROPERTY = "bytechef.ai.mcp.server.enabled";
    private static final String COPILOT_ENABLED_PROPERTY = "bytechef.ai.copilot.enabled";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment environment = context.getEnvironment();

        String mcpServerEnabled = environment.getProperty(MCP_SERVER_ENABLED_PROPERTY);
        String copilotEnabled = environment.getProperty(COPILOT_ENABLED_PROPERTY);

        return "true".equalsIgnoreCase(mcpServerEnabled) || "true".equalsIgnoreCase(copilotEnabled);
    }
}
