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
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
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
class McpComponentOwnershipResolverIntTest {

    @Autowired
    private McpComponentOwnershipResolver mcpComponentOwnershipResolver;

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private WorkspaceMcpServerRepository workspaceMcpServerRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private McpServer mcpServer;
    private long workspaceId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("mcp-component-ownership"));

        workspaceId = workspace.getId();

        mcpServer = mcpServerRepository.save(
            new McpServer("mcp-component-ownership", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

        workspaceMcpServerRepository.save(new WorkspaceMcpServer(mcpServer.getId(), workspaceId));
    }

    @AfterEach
    void afterEach() {
        mcpComponentRepository.deleteAll();
        workspaceMcpServerRepository.deleteAll();
        mcpServerRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    @Test
    void testResourceType() {
        assertThat(mcpComponentOwnershipResolver.resourceType()).isEqualTo("McpComponent");
    }

    @Test
    void testResolvesWorkspaceViaServer() {
        McpComponent mcpComponent = mcpComponentRepository.save(
            new McpComponent("testComponent", 1, mcpServer.getId(), null));

        ResourceOwner resourceOwner = mcpComponentOwnershipResolver.resolveOwner(mcpComponent.getId());

        assertThat(resourceOwner.workspaceId()).hasValue(workspaceId);
    }

    @Test
    void testUnknownIsUnknown() {
        ResourceOwner resourceOwner = mcpComponentOwnershipResolver.resolveOwner(Long.MAX_VALUE);

        assertThat(resourceOwner.workspaceId()).isEmpty();
    }
}
