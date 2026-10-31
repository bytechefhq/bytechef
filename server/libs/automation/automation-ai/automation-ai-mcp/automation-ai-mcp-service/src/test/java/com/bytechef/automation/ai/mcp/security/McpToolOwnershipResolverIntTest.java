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

package com.bytechef.automation.ai.mcp.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfiguration;
import com.bytechef.automation.ai.mcp.config.McpProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.ai.mcp.domain.WorkspaceMcpServer;
import com.bytechef.automation.ai.mcp.repository.WorkspaceMcpServerRepository;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.repository.McpToolRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = McpProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@McpProjectIntTestConfigurationSharedMocks
class McpToolOwnershipResolverIntTest {

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private McpToolOwnershipResolver mcpToolOwnershipResolver;

    @Autowired
    private McpToolRepository mcpToolRepository;

    @Autowired
    private WorkspaceMcpServerRepository workspaceMcpServerRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private McpComponent mcpComponent;
    private long workspaceId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("mcp-tool-ownership"));

        workspaceId = workspace.getId();

        McpServer mcpServer = mcpServerRepository.save(
            new McpServer("mcp-tool-ownership", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

        workspaceMcpServerRepository.save(new WorkspaceMcpServer(mcpServer.getId(), workspaceId));

        mcpComponent = mcpComponentRepository.save(new McpComponent("testComponent", 1, mcpServer.getId(), null));
    }

    @AfterEach
    void afterEach() {
        mcpToolRepository.deleteAll();
        mcpComponentRepository.deleteAll();
        workspaceMcpServerRepository.deleteAll();
        mcpServerRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    void testResourceType() {
        assertThat(mcpToolOwnershipResolver.resourceType()).isEqualTo("McpTool");
    }

    @Test
    void testResolvesWorkspaceViaComponentAndServer() {
        McpTool mcpTool = mcpToolRepository.save(new McpTool("testTool", Map.of(), mcpComponent.getId()));

        ResourceOwner resourceOwner = mcpToolOwnershipResolver.resolveOwner(mcpTool.getId());

        assertThat(resourceOwner.workspaceId()).hasValue(workspaceId);
    }

    @Test
    void testUnknownIsUnknown() {
        ResourceOwner resourceOwner = mcpToolOwnershipResolver.resolveOwner(Long.MAX_VALUE);

        assertThat(resourceOwner.workspaceId()).isEmpty();
    }
}
