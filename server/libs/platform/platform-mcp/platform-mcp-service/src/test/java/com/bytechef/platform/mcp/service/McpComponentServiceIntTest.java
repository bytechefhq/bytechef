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
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.mcp.config.PlatformMcpIntTestConfiguration;
import com.bytechef.platform.mcp.config.PlatformMcpMethodSecurityTestConfiguration;
import com.bytechef.platform.mcp.config.PlatformMcpMethodSecurityTestConfiguration.TenantAdminCheck;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
class McpComponentServiceIntTest {

    @Autowired
    private ConnectionRepository connectionRepository;

    @MockitoBean
    private MailService mailService;

    @Autowired
    private McpComponentService mcpComponentService;

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @MockitoBean
    private WorkflowService workflowService;

    private McpServer mcpServer;

    @BeforeEach
    void beforeEach() {
        mcpServer = mcpServerRepository.save(
            new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT));
    }

    @AfterEach
    void afterEach() {
        mcpComponentRepository.deleteAll();
        mcpServerRepository.deleteAll();
        connectionRepository.deleteAll();
    }

    @Test
    void testCreate() {
        McpComponent mcpComponent = getMcpComponent();

        mcpComponent = mcpComponentService.create(mcpComponent);

        assertThat(mcpComponent)
            .hasFieldOrPropertyWithValue("componentName", "test-component")
            .hasFieldOrPropertyWithValue("componentVersion", 1)
            .hasFieldOrPropertyWithValue("mcpServerId", mcpServer.getId())
            .hasFieldOrPropertyWithValue("connectionId", null);
        assertThat(mcpComponent.getId()).isNotNull();
    }

    @Test
    void testUpdate() {
        McpComponent mcpComponent = mcpComponentRepository.save(getMcpComponent());

        Connection connection = new Connection();

        connection.setComponentName("test-connection-component");
        connection.setConnectionVersion(1);
        connection.setId(2L);
        connection.setName("test-connection");
        connection.setParameters(Map.of("param1", "value1", "param2", "value2"));

        connectionRepository.save(connection);

        mcpComponent.setConnectionId(2L);

        mcpComponent = mcpComponentService.update(mcpComponent, null);

        assertThat(mcpComponent)
            .hasFieldOrPropertyWithValue("connectionId", 2L)
            .hasFieldOrPropertyWithValue("mcpServerId", mcpServer.getId());
    }

    @Test
    void testDelete() {
        McpComponent mcpComponent = mcpComponentRepository.save(getMcpComponent());

        mcpComponentService.delete(Validate.notNull(mcpComponent.getId(), "id"));

        assertThat(mcpComponentRepository.findById(mcpComponent.getId()))
            .isNotPresent();
    }

    @Test
    void testGetMcpComponent() {
        McpComponent mcpComponent = mcpComponentRepository.save(getMcpComponent());

        McpComponent retrievedComponent = mcpComponentService.getMcpComponent(
            Validate.notNull(mcpComponent.getId(), "id"));

        assertThat(retrievedComponent).isEqualTo(mcpComponent);
    }

    @Test
    void testGetMcpComponents() {
        McpComponent mcpComponent = mcpComponentRepository.save(getMcpComponent());

        List<McpComponent> components = mcpComponentService.getMcpComponents();

        assertThat(components).hasSize(1);
        assertThat(components.getFirst()).isEqualTo(mcpComponent);
    }

    @Test
    void testGetMcpServerMcpComponents() {
        McpComponent component1 = mcpComponentRepository.save(getMcpComponent());
        McpComponent component2 = new McpComponent("test-component-2", 1, mcpServer.getId(), null);

        component2 = mcpComponentRepository.save(component2);

        McpServer anotherServer = mcpServerRepository.save(
            new McpServer("another-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

        McpComponent anotherComponent = new McpComponent("another-component", 1, anotherServer.getId(), null);

        mcpComponentRepository.save(anotherComponent);

        List<McpComponent> serverComponents = mcpComponentService.getMcpServerMcpComponents(mcpServer.getId());
        List<McpComponent> anotherServerComponents =
            mcpComponentService.getMcpServerMcpComponents(anotherServer.getId());

        assertThat(serverComponents).hasSize(2);
        assertThat(serverComponents).containsExactlyInAnyOrder(component1, component2);

        assertThat(anotherServerComponents).hasSize(1);

        McpComponent first = anotherServerComponents.getFirst();

        assertThat(first.getComponentName()).isEqualTo("another-component");

        assertThat(mcpComponentService.getMcpServerMcpComponents(Long.MAX_VALUE)).isEmpty();
    }

    private McpComponent getMcpComponent() {
        return new McpComponent("test-component", 1, mcpServer.getId(), null);
    }

    @Nested
    class UpdateRequiredAuthorities {

        @Test
        void testUpdateKeepsRequiredAuthoritiesWhenOmitted() {
            McpComponent existingMcpComponent = saveMcpComponentWithRequiredAuthorities(Set.of("ROLE_ADMIN"));

            McpComponent updatedMcpComponent = mcpComponentService.update(
                getMcpComponentUpdate(existingMcpComponent), null);

            assertThat(updatedMcpComponent.getRequiredAuthorities()).containsExactly("ROLE_ADMIN");
            assertThat(getStoredRequiredAuthorities(existingMcpComponent)).containsExactly("ROLE_ADMIN");
        }

        @Test
        void testUpdateClearsRequiredAuthoritiesWhenEmpty() {
            McpComponent existingMcpComponent = saveMcpComponentWithRequiredAuthorities(Set.of("ROLE_ADMIN"));

            McpComponent updatedMcpComponent = mcpComponentService.update(
                getMcpComponentUpdate(existingMcpComponent), Set.of());

            assertThat(updatedMcpComponent.getRequiredAuthorities()).isEmpty();
            assertThat(getStoredRequiredAuthorities(existingMcpComponent)).isEmpty();
        }

        @Test
        void testUpdateReplacesRequiredAuthorities() {
            McpComponent existingMcpComponent = saveMcpComponentWithRequiredAuthorities(Set.of("ROLE_ADMIN"));

            McpComponent updatedMcpComponent = mcpComponentService.update(
                getMcpComponentUpdate(existingMcpComponent), Set.of("ROLE_REVIEWER"));

            assertThat(updatedMcpComponent.getRequiredAuthorities()).containsExactly("ROLE_REVIEWER");
            assertThat(getStoredRequiredAuthorities(existingMcpComponent)).containsExactly("ROLE_REVIEWER");
        }

        private McpComponent getMcpComponentUpdate(McpComponent existingMcpComponent) {
            McpComponent mcpComponent = new McpComponent(
                existingMcpComponent.getComponentName(), existingMcpComponent.getComponentVersion(),
                existingMcpComponent.getMcpServerId(), null, existingMcpComponent.getVersion());

            mcpComponent.setId(existingMcpComponent.getId());

            return mcpComponent;
        }

        private Set<String> getStoredRequiredAuthorities(McpComponent mcpComponent) {
            McpComponent storedMcpComponent = mcpComponentRepository.findById(mcpComponent.getId())
                .orElseThrow();

            return storedMcpComponent.getRequiredAuthorities();
        }

        private McpComponent saveMcpComponentWithRequiredAuthorities(Set<String> requiredAuthorities) {
            McpComponent mcpComponent = getMcpComponent();

            mcpComponent.setRequiredAuthorities(requiredAuthorities);

            return mcpComponentRepository.save(mcpComponent);
        }
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
        void testCreateMcpComponentServiceRequiresServerEditor() {
            McpComponent mcpComponent = new McpComponent("component", 1, 3L, null);

            assertThatThrownBy(() -> mcpComponentService.create(mcpComponent))
                .isInstanceOf(AccessDeniedException.class);

            verify(permissionEvaluator).hasPermission(any(), eq(3L), eq("McpServer"), eq("MCP_EDIT"));
        }

        @Test
        void testUpdateIsDeniedWhenTheComponentBelongsToAnotherServer() {
            McpComponent existingMcpComponent = mcpComponentRepository.save(getMcpComponent());

            McpServer otherMcpServer = mcpServerRepository.save(
                new McpServer("other-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

            long otherMcpServerId = Validate.notNull(otherMcpServer.getId(), "id");

            when(permissionEvaluator.hasPermission(any(), eq(otherMcpServerId), eq("McpServer"), eq("MCP_EDIT")))
                .thenReturn(true);

            McpComponent mcpComponent = new McpComponent("test-component", 1, otherMcpServerId, null);

            mcpComponent.setId(existingMcpComponent.getId());

            assertThatThrownBy(() -> mcpComponentService.update(mcpComponent, Set.of("ROLE_TAMPERED")))
                .isInstanceOf(AccessDeniedException.class);

            McpComponent storedMcpComponent = mcpComponentRepository.findById(existingMcpComponent.getId())
                .orElseThrow();

            assertThat(storedMcpComponent.getRequiredAuthorities()).isEmpty();
        }

        @Test
        void testUpdateIsAllowedForAnEditorOfTheComponent() {
            McpComponent existingMcpComponent = mcpComponentRepository.save(getMcpComponent());

            long mcpComponentId = Validate.notNull(existingMcpComponent.getId(), "id");

            when(permissionEvaluator.hasPermission(any(), eq(mcpComponentId), eq("McpComponent"), eq("MCP_EDIT")))
                .thenReturn(true);

            McpComponent updatedMcpComponent = mcpComponentService.update(
                existingMcpComponent, Set.of("ROLE_REVIEWER"));

            assertThat(updatedMcpComponent.getRequiredAuthorities()).containsExactly("ROLE_REVIEWER");
        }

        @Test
        void testGetMcpComponentsRequiresTenantAdmin() {
            assertThatThrownBy(() -> mcpComponentService.getMcpComponents())
                .isInstanceOf(AccessDeniedException.class);

            verify(tenantAdminCheck).isTenantAdmin();
        }
    }
}
