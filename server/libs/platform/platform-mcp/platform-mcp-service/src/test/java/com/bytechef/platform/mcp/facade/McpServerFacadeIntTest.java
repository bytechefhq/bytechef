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

package com.bytechef.platform.mcp.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
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
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
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
class McpServerFacadeIntTest {

    @MockitoBean
    private MailService mailService;

    @Autowired
    private McpComponentService mcpComponentService;

    private McpServerFacade mcpServerFacade;

    @Autowired
    private McpServerService mcpServerService;

    @Autowired
    private McpToolService mcpToolService;

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private McpToolRepository mcpToolRepository;

    @Autowired
    private TagService tagService;

    @MockitoBean
    private WorkflowService workflowService;

    private McpServer mcpServer;

    @BeforeEach
    void beforeEach() {
        mcpServerFacade = new McpServerFacadeImpl(mcpComponentService, mcpServerService, mcpToolService, tagService);

        mcpServer = mcpServerRepository.save(
            new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT));
    }

    @AfterEach
    void afterEach() {
        mcpToolRepository.deleteAll();
        mcpComponentRepository.deleteAll();
        mcpServerRepository.deleteAll();
    }

    @Test
    void testCreateMcpComponentWithTools() {
        McpComponent mcpComponent = getMcpComponent();

        List<McpTool> mcpTools = List.of(getMcpTool("tool1"), getMcpTool("tool2"));

        McpComponent createdComponent = mcpServerFacade.create(mcpComponent, mcpTools);

        assertThat(createdComponent).isNotNull();
        assertThat(createdComponent.getId()).isNotNull();
        assertThat(createdComponent.getComponentName()).isEqualTo("test-component");
        assertThat(createdComponent.getMcpServerId()).isEqualTo(mcpServer.getId());

        List<McpTool> savedTools = mcpToolRepository.findAllByMcpComponentId(createdComponent.getId());

        assertThat(savedTools).hasSize(2);
        assertThat(savedTools).extracting("name")
            .containsExactlyInAnyOrder("tool1", "tool2");
    }

    @Test
    void testCreateMcpComponentWithEmptyTools() {
        McpComponent mcpComponent = getMcpComponent();

        McpComponent createdComponent = mcpServerFacade.create(mcpComponent, List.of());

        assertThat(createdComponent).isNotNull();
        assertThat(createdComponent.getId()).isNotNull();
        assertThat(createdComponent.getComponentName()).isEqualTo("test-component");

        List<McpTool> savedTools = mcpToolRepository.findAllByMcpComponentId(createdComponent.getId());

        assertThat(savedTools).isEmpty();
    }

    @Test
    void testUpdateMcpComponentWithTools() {
        McpComponent mcpComponent = mcpComponentRepository.save(getMcpComponent());

        McpTool originalTool = getMcpTool("original-tool", mcpComponent.getId());

        originalTool.setMcpComponentId(mcpComponent.getId());

        mcpToolRepository.save(originalTool);

        List<McpTool> newTools = List.of(
            getMcpTool("new-tool1", mcpComponent.getId()), getMcpTool("new-tool2", mcpComponent.getId()));

        mcpComponent.setComponentName("updated-component");

        McpComponent updatedComponent = mcpServerFacade.update(mcpComponent, newTools);

        assertThat(updatedComponent).isNotNull();
        assertThat(updatedComponent.getComponentName()).isEqualTo("test-component");

        List<McpTool> savedTools = mcpToolRepository.findAllByMcpComponentId(updatedComponent.getId());

        assertThat(savedTools).hasSize(2);
        assertThat(savedTools).extracting("name")
            .containsExactlyInAnyOrder("new-tool1", "new-tool2");
    }

    @Test
    void testUpdateMcpComponentKeepsToolEnabledState() {
        McpComponent mcpComponent = mcpComponentRepository.save(getMcpComponent());

        McpTool disabledTool = getMcpTool("kept-tool", mcpComponent.getId());

        disabledTool.setEnabled(false);

        mcpToolRepository.save(disabledTool);

        List<McpTool> newTools = List.of(
            getMcpTool("kept-tool", mcpComponent.getId()), getMcpTool("added-tool", mcpComponent.getId()));

        McpComponent updatedComponent = mcpServerFacade.update(mcpComponent, newTools);

        List<McpTool> savedTools = mcpToolRepository.findAllByMcpComponentId(updatedComponent.getId());

        assertThat(savedTools).extracting(McpTool::getName, McpTool::isEnabled)
            .containsExactlyInAnyOrder(tuple("kept-tool", false), tuple("added-tool", true));
    }

    @Test
    void testDeleteMcpComponent() {
        McpComponent mcpComponent = mcpComponentRepository.save(getMcpComponent());

        McpTool mcpTool = getMcpTool("test-tool", mcpComponent.getId());

        mcpTool.setMcpComponentId(mcpComponent.getId());

        mcpToolRepository.save(mcpTool);

        mcpServerFacade.deleteMcpComponent(Validate.notNull(mcpComponent.getId(), "id"));

        assertThat(mcpComponentRepository.findById(mcpComponent.getId())).isNotPresent();
        assertThat(mcpToolRepository.findAllByMcpComponentId(mcpComponent.getId())).isEmpty();
    }

    @Test
    void testDeleteMcpServer() {
        McpComponent mcpComponent = getMcpComponent();

        mcpComponentRepository.save(mcpComponent);

        mcpServerFacade.deleteMcpServer(Validate.notNull(mcpServer.getId(), "id"));

        assertThat(mcpServerRepository.findById(mcpServer.getId())).isNotPresent();
        assertThat(mcpComponentRepository.findAllByMcpServerId(mcpServer.getId())).isEmpty();
    }

    @Test
    void testGetMcpServerMcpComponents() {
        McpComponent component1 = getMcpComponent();

        component1.setComponentName("component1");

        mcpComponentRepository.save(component1);

        McpComponent component2 = getMcpComponent();

        component2.setComponentName("component2");

        mcpComponentRepository.save(component2);

        McpServer anotherServer = mcpServerRepository.save(
            new McpServer("another-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT));

        McpComponent anotherComponent = getMcpComponent();

        anotherComponent.setMcpServerId(anotherServer.getId());
        anotherComponent.setComponentName("another-component");

        mcpComponentRepository.save(anotherComponent);

        List<McpServer> servers = List.of(mcpServer, anotherServer);

        Map<McpServer, List<McpComponent>> serverComponents = mcpServerFacade.getMcpServerMcpComponents(servers);

        assertThat(serverComponents).hasSize(2);
        assertThat(serverComponents.get(mcpServer)).hasSize(2);
        assertThat(serverComponents.get(anotherServer)).hasSize(1);
        assertThat(serverComponents.get(mcpServer)).extracting("componentName")
            .containsExactlyInAnyOrder("component1", "component2");
        assertThat(serverComponents.get(anotherServer)).extracting("componentName")
            .containsExactly("another-component");
    }

    @Test
    void testUpdateMcpServerTags() {
        List<Tag> tags = List.of(new Tag("tag1"), new Tag("tag2"));

        List<Tag> savedTags = mcpServerFacade.updateMcpServerTags(mcpServer.getId(), tags);

        assertThat(savedTags).hasSize(2);
        assertThat(savedTags).extracting("name")
            .containsExactlyInAnyOrder("tag1", "tag2");
        assertThat(savedTags).allMatch(tag -> tag.getId() != null);
    }

    private McpComponent getMcpComponent() {
        return new McpComponent("test-component", 1, mcpServer.getId(), null);
    }

    private McpTool getMcpTool(String name) {
        return new McpTool(name, Map.of());
    }

    private McpTool getMcpTool(String name, long mcpComponentId) {
        return new McpTool(name, Map.of(), mcpComponentId);
    }

    @Nested
    @Import({
        PlatformMcpMethodSecurityTestConfiguration.class, McpServerFacadeIntTest.MethodSecurityFacadeConfiguration.class
    })
    class MethodSecurity {

        @Autowired
        private PermissionEvaluator permissionEvaluator;

        @Autowired
        private TenantAdminCheck tenantAdminCheck;

        @Autowired
        private McpServerFacade securedMcpServerFacade;

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
        void testDeleteMcpServerRequiresEditor() {
            assertThatThrownBy(() -> securedMcpServerFacade.deleteMcpServer(3L))
                .isInstanceOf(AccessDeniedException.class);

            verify(permissionEvaluator).hasPermission(any(), eq(3L), eq("McpServer"), eq("MCP_EDIT"));
        }

        @Test
        void testUpdateMcpServerTagsRequiresEditor() {
            assertThatThrownBy(() -> securedMcpServerFacade.updateMcpServerTags(3L, List.of()))
                .isInstanceOf(AccessDeniedException.class);

            verify(permissionEvaluator).hasPermission(any(), eq(3L), eq("McpServer"), eq("MCP_EDIT"));
        }

        @Test
        void testCreateMcpComponentRequiresServerEditor() {
            McpComponent mcpComponent = new McpComponent("component", 1, 3L, null);

            assertThatThrownBy(() -> securedMcpServerFacade.create(mcpComponent, List.of()))
                .isInstanceOf(AccessDeniedException.class);

            verify(permissionEvaluator).hasPermission(any(), eq(3L), eq("McpServer"), eq("MCP_EDIT"));
        }

        @Test
        void testUpdateMcpComponentRequiresEditor() {
            McpComponent mcpComponent = new McpComponent("component", 1, 3L, null);

            mcpComponent.setId(4L);

            assertThatThrownBy(() -> securedMcpServerFacade.update(mcpComponent, List.of()))
                .isInstanceOf(AccessDeniedException.class);

            verify(permissionEvaluator).hasPermission(any(), eq(4L), eq("McpComponent"), eq("MCP_EDIT"));
        }

        @Test
        void testDeleteMcpComponentRequiresEditor() {
            assertThatThrownBy(() -> securedMcpServerFacade.deleteMcpComponent(4L))
                .isInstanceOf(AccessDeniedException.class);

            verify(permissionEvaluator).hasPermission(any(), eq(4L), eq("McpComponent"), eq("MCP_EDIT"));
        }
    }

    static class MethodSecurityFacadeConfiguration {

        @Bean
        McpServerFacade securedMcpServerFacade(
            McpComponentService mcpComponentService, McpServerService mcpServerService, McpToolService mcpToolService,
            TagService tagService) {

            return new McpServerFacadeImpl(mcpComponentService, mcpServerService, mcpToolService, tagService);
        }
    }
}
