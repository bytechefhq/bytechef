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

package com.bytechef.platform.oauth2.authorizationserver.service;

import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.oauth2.authorizationserver.annotation.ConditionalOnOauth2AuthorizationServerEnabled;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Resolves the display name of the MCP server an OAuth2 {@code resource} URL points to, falling back to the URL.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnOauth2AuthorizationServerEnabled
public class McpResourceNameResolver {

    private static final String MANAGEMENT_NAME = "Management";
    private static final Pattern AUTOMATION_PATTERN = Pattern.compile("/api/automation/([^/]+)/mcp/?$");
    private static final Pattern MANAGEMENT_PATTERN = Pattern.compile("/api/management(?:/mcp|/[^/]+/mcp)/?$");

    private final ObjectProvider<McpServerService> mcpServerServiceProvider;

    @SuppressFBWarnings("EI2")
    public McpResourceNameResolver(ObjectProvider<McpServerService> mcpServerServiceProvider) {
        this.mcpServerServiceProvider = mcpServerServiceProvider;
    }

    public String resolveName(String resource) {
        if (MANAGEMENT_PATTERN.matcher(resource)
            .find()) {

            return MANAGEMENT_NAME;
        }

        Matcher automationMatcher = AUTOMATION_PATTERN.matcher(resource);
        McpServerService mcpServerService = mcpServerServiceProvider.getIfAvailable();

        if (!automationMatcher.find() || mcpServerService == null) {
            return resource;
        }

        try {
            McpServer mcpServer = mcpServerService.getMcpServer(automationMatcher.group(1));

            return mcpServer.getName();
        } catch (RuntimeException runtimeException) {
            return resource;
        }
    }
}
