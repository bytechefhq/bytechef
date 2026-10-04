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

package com.bytechef.platform.mcp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.mcp.config.PlatformMcpIntTestConfiguration;
import com.bytechef.platform.mcp.config.PlatformMcpMethodSecurityTestConfiguration;
import com.bytechef.platform.mcp.config.PlatformMcpMethodSecurityTestConfiguration.TenantAdminCheck;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.repository.McpToolRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformMcpIntTestConfiguration.class)
class McpToolServiceIntTest {

    @MockitoBean
    private MailService mailService;

    @Autowired
    private McpToolService mcpToolService;

    @Autowired
    private McpToolRepository mcpToolRepository;

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @MockitoBean
    private WorkflowService workflowService;

    private McpComponent mcpComponent;

    @BeforeEach
    void beforeEach() {
        McpServer mcpServer = mcpServerRepository.save(
            new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

        mcpComponent = mcpComponentRepository.save(new McpComponent("test-component", 1, mcpServer.getId(), null));
    }

    @AfterEach
    void afterEach() {
        mcpToolRepository.deleteAll();
        mcpComponentRepository.deleteAll();
        mcpServerRepository.deleteAll();
    }

    @Test
    void testCreate() {
        McpTool mcpTool = getMcpTool();

        mcpTool = mcpToolService.create(mcpTool);

        assertThat(mcpTool)
            .hasFieldOrPropertyWithValue("name", "test-tool")
            .hasFieldOrPropertyWithValue("mcpComponentId", mcpComponent.getId());
        assertThat(mcpTool.getId()).isNotNull();
        assertThat(mcpTool.getParameters()).isEqualTo(Map.of("param1", "value1"));
    }

    @Test
    void testUpdate() {
        McpTool mcpTool = mcpToolRepository.save(getMcpTool());

        mcpTool.setName("updated-tool");

        mcpTool = mcpToolService.update(mcpTool);

        assertThat(mcpTool)
            .hasFieldOrPropertyWithValue("name", "updated-tool")
            .hasFieldOrPropertyWithValue("mcpComponentId", mcpComponent.getId());
    }

    @Test
    void testCreateDefaultsToEnabled() {
        McpTool mcpTool = mcpToolService.create(getMcpTool());

        assertThat(mcpTool.isEnabled()).isTrue();
    }

    @Test
    void testUpdateEnabled() {
        McpTool mcpTool = mcpToolRepository.save(getMcpTool());

        mcpToolService.updateEnabled(mcpTool.getId(), false);

        assertThat(mcpToolRepository.findById(mcpTool.getId()))
            .get()
            .extracting(McpTool::isEnabled)
            .isEqualTo(false);
    }

    @Test
    void testDelete() {
        McpTool mcpTool = mcpToolRepository.save(getMcpTool());

        mcpToolService.delete(Validate.notNull(mcpTool, "mcpTool"));

        assertThat(mcpToolRepository.findById(mcpTool.getId()))
            .isNotPresent();
    }

    @Test
    void testFetchMcpTool() {
        McpTool mcpTool = mcpToolRepository.save(getMcpTool());

        Optional<McpTool> fetchedTool = mcpToolService.fetchMcpTool(Validate.notNull(mcpTool.getId(), "id"));

        assertThat(fetchedTool).isPresent();
        assertThat(fetchedTool.get()).isEqualTo(mcpTool);
    }

    @Test
    void testGetMcpTools() {
        McpTool mcpTool = mcpToolRepository.save(getMcpTool());

        assertThat(mcpToolService.getMcpTools()).hasSize(1);
        assertThat(mcpToolService.getMcpTools()
            .getFirst()).isEqualTo(mcpTool);
    }

    @Test
    void testGetMcpComponentMcpTools() {
        McpTool mcpTool = mcpToolRepository.save(getMcpTool());

        assertThat(mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())).hasSize(1);
        assertThat(mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())
            .getFirst()).isEqualTo(mcpTool);

        assertThat(mcpToolService.getMcpComponentMcpTools(Long.MAX_VALUE)).hasSize(0);
    }

    private McpTool getMcpTool() {
        return new McpTool("test-tool", Map.of("param1", "value1"), mcpComponent.getId());
    }

    @Nested
    @Import(PlatformMcpMethodSecurityTestConfiguration.class)
    class MethodSecurity {

        @Autowired
        private PermissionEvaluator permissionEvaluator;

        @Autowired
        private TenantAdminCheck tenantAdminCheck;

        @BeforeEach
        void setAuthentication() {
            SecurityContextHolder.getContext()
                .setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        "viewer", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        }

        @AfterEach
        void clearAuthentication() {
            SecurityContextHolder.clearContext();

            reset(permissionEvaluator, tenantAdminCheck);
        }

        @Test
        void testCreateMcpToolRequiresComponentEditor() {
            McpTool mcpTool = new McpTool("tool", Map.of(), 5L);

            assertThatThrownBy(() -> mcpToolService.create(mcpTool))
                .isInstanceOf(AccessDeniedException.class);

            verify(permissionEvaluator).hasPermission(any(), eq(5L), eq("McpComponent"), eq("MCP_EDIT"));
        }

        @Test
        void testFetchMcpToolRequiresViewer() {
            assertThatThrownBy(() -> mcpToolService.fetchMcpTool(9L))
                .isInstanceOf(AccessDeniedException.class);

            verify(permissionEvaluator).hasPermission(any(), eq(9L), eq("McpTool"), eq("MCP_VIEW"));
        }

        @Test
        void testGetMcpToolsRequiresTenantAdmin() {
            assertThatThrownBy(() -> mcpToolService.getMcpTools())
                .isInstanceOf(AccessDeniedException.class);

            verify(tenantAdminCheck).isTenantAdmin();
        }

        @Test
        void testUpdateMcpToolRequiresEditor() {
            McpTool mcpTool = new McpTool(9L, "tool", Map.of(), 5L);

            assertThatThrownBy(() -> mcpToolService.update(mcpTool))
                .isInstanceOf(AccessDeniedException.class);

            verify(permissionEvaluator).hasPermission(any(), eq(9L), eq("McpTool"), eq("MCP_EDIT"));
        }

        @Test
        void testDeleteMcpToolRequiresEditor() {
            McpTool mcpTool = new McpTool(9L, "tool", Map.of(), 5L);

            assertThatThrownBy(() -> mcpToolService.delete(mcpTool))
                .isInstanceOf(AccessDeniedException.class);

            verify(permissionEvaluator).hasPermission(any(), eq(9L), eq("McpTool"), eq("MCP_EDIT"));
        }
    }
}
