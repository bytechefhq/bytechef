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

package com.bytechef.automation.ai.mcp.server.config;

import static org.mockito.Mockito.mock;

import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * @author Ivica Cardic
 */
@EnableMethodSecurity
@ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
@Configuration
public class AutomationMcpServerMethodSecurityIntTestConfiguration {

    @Bean
    McpProjectService mcpProjectService() {
        return mock(GuardedMcpProjectService.class);
    }

    @Bean
    McpServerService mcpServerService() {
        return mock(GuardedMcpServerService.class);
    }

    @Bean
    McpToolService mcpToolService() {
        return mock(GuardedMcpToolService.class);
    }

    @Bean
    PermissionService permissionService() {
        return mock(PermissionService.class);
    }

    public abstract static class GuardedMcpProjectService implements McpProjectService {

        @Override
        @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_VIEW')")
        public abstract List<McpProject> getMcpServerMcpProjects(long mcpServerId);
    }

    public abstract static class GuardedMcpServerService implements McpServerService {

        @Override
        @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_VIEW')")
        public abstract McpServer getMcpServer(long mcpServerId);
    }

    public abstract static class GuardedMcpToolService implements McpToolService {

        @Override
        @PreAuthorize("hasPermission(#mcpToolId, 'McpTool', 'MCP_VIEW')")
        public abstract Optional<McpTool> fetchMcpTool(long mcpToolId);
    }
}
