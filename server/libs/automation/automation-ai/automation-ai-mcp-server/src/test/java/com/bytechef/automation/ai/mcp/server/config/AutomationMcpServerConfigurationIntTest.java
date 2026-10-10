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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.mcp.server.facade.AutomationMcpToolFacade;
import com.bytechef.automation.ai.mcp.server.spi.McpServerWorkspaceToolCallbackContributor;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import io.modelcontextprotocol.server.McpServerFeatures;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.AopTestUtils;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AutomationMcpServerMethodSecurityIntTestConfiguration.class)
class AutomationMcpServerConfigurationIntTest {

    @Autowired
    private McpProjectService mcpProjectService;

    @Autowired
    private McpServerService mcpServerService;

    @Autowired
    private McpToolService mcpToolService;

    private McpComponentService mcpComponentService;
    private McpProjectService mcpProjectServiceTarget;
    private McpServerService mcpServerServiceTarget;
    private AutomationMcpToolFacade mcpToolFacade;
    private McpToolService mcpToolServiceTarget;
    private WorkspaceMcpServerService workspaceMcpServerService;
    private ObjectProvider<McpServerWorkspaceToolCallbackContributor> workspaceToolProviders;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    "viewer", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        mcpProjectServiceTarget = AopTestUtils.getUltimateTargetObject(mcpProjectService);
        mcpServerServiceTarget = AopTestUtils.getUltimateTargetObject(mcpServerService);
        mcpToolServiceTarget = AopTestUtils.getUltimateTargetObject(mcpToolService);

        reset(mcpProjectServiceTarget, mcpServerServiceTarget, mcpToolServiceTarget);

        mcpComponentService = mock(McpComponentService.class);
        mcpToolFacade = mock(AutomationMcpToolFacade.class);
        workspaceMcpServerService = mock(WorkspaceMcpServerService.class);
        workspaceToolProviders = (ObjectProvider<McpServerWorkspaceToolCallbackContributor>) mock(
            ObjectProvider.class);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testMcpServerProjectsAreDeniedOutsideTheToolListing() {
        assertThatThrownBy(() -> mcpProjectService.getMcpServerMcpProjects(1L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testMcpServerProjectsAreListedWithoutWorkspacePermissionChecks() {
        McpServer mcpServer = new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        mcpServer.setId(1L);

        when(mcpServerServiceTarget.getMcpServer("secret")).thenReturn(mcpServer);
        when(mcpComponentService.getMcpServerMcpComponents(1L)).thenReturn(List.of());
        when(mcpProjectServiceTarget.getMcpServerMcpProjects(1L)).thenReturn(List.of());
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(1L)).thenReturn(Optional.empty());

        List<McpServerFeatures.AsyncToolSpecification> toolSpecifications = buildToolSpecifications(Set.of());

        assertThat(toolSpecifications).isEmpty();
        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
        verify(mcpProjectServiceTarget).getMcpServerMcpProjects(1L);
    }

    @Test
    void testEnforcingServerKeepsOnlyAuthorizedComponents() {
        McpServer mcpServer = enforcingServer();

        McpComponent salesforce = component("salesforce", Set.of("ROLE_SALES"));
        McpComponent admin = component("admin", Set.of("ROLE_ADMIN"));

        List<McpComponent> authorized = AutomationMcpServerConfiguration.authorizedComponents(
            mcpServer, List.of(salesforce, admin), Set.of("ROLE_USER", "ROLE_SALES"));

        assertThat(authorized).containsExactly(salesforce);
    }

    @Test
    void testEnforcingServerDeniesComponentWithoutGrantingAuthority() {
        McpServer mcpServer = enforcingServer();

        McpComponent admin = component("admin", Set.of("ROLE_ADMIN"));

        List<McpComponent> authorized = AutomationMcpServerConfiguration.authorizedComponents(
            mcpServer, List.of(admin), Set.of("ROLE_USER"));

        assertThat(authorized).isEmpty();
    }

    @Test
    void testNonEnforcingServerExposesAllComponents() {
        McpServer mcpServer = new McpServer();

        McpComponent salesforce = component("salesforce", Set.of("ROLE_SALES"));
        McpComponent admin = component("admin", Set.of("ROLE_ADMIN"));

        List<McpComponent> authorized = AutomationMcpServerConfiguration.authorizedComponents(
            mcpServer, List.of(salesforce, admin), Set.of());

        assertThat(authorized).containsExactly(salesforce, admin);
    }

    @Test
    void testEnforcingServerDeniesToolSourcesWithoutGrantingAuthorities() {
        McpServer mcpServer = enforcingServer();

        assertThat(
            AutomationMcpServerConfiguration.isUnscopedToolSourceAuthorized(mcpServer, Set.of("ROLE_ADMIN")))
                .isFalse();
    }

    @Test
    void testNonEnforcingServerAllowsToolSourcesWithoutGrantingAuthorities() {
        McpServer mcpServer = new McpServer();

        assertThat(AutomationMcpServerConfiguration.isUnscopedToolSourceAuthorized(mcpServer, Set.of()))
            .isTrue();
    }

    @Test
    void testEnforcingServerDoesNotListWorkflowBackedTools() {
        McpServer mcpServer = enforcingServer();

        mcpServer.setId(1L);

        when(mcpServerServiceTarget.getMcpServer("secret")).thenReturn(mcpServer);
        when(mcpComponentService.getMcpServerMcpComponents(1L)).thenReturn(List.of());

        List<McpServerFeatures.AsyncToolSpecification> toolSpecifications =
            buildToolSpecifications(Set.of("ROLE_ADMIN"));

        assertThat(toolSpecifications).isEmpty();
        verify(mcpProjectServiceTarget, never()).getMcpServerMcpProjects(1L);
        verify(workspaceMcpServerService, never()).fetchWorkspaceIdByMcpServerId(1L);
    }

    @Test
    void testDisabledMcpServerServesNoTools() {
        McpServer mcpServer = new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT, false);

        mcpServer.setId(1L);

        when(mcpServerServiceTarget.getMcpServer("secret")).thenReturn(mcpServer);

        List<McpServerFeatures.AsyncToolSpecification> toolSpecifications = buildToolSpecifications(Set.of());

        assertThat(toolSpecifications).isEmpty();
        verify(mcpComponentService, never()).getMcpServerMcpComponents(1L);
        verify(mcpProjectServiceTarget, never()).getMcpServerMcpProjects(1L);
        verify(workspaceMcpServerService, never()).fetchWorkspaceIdByMcpServerId(1L);
    }

    @Test
    void testEmbeddedMcpServerServesNoTools() {
        McpServer mcpServer = new McpServer("test-server", PlatformType.EMBEDDED, Environment.DEVELOPMENT);

        mcpServer.setId(1L);

        when(mcpServerServiceTarget.getMcpServer("secret")).thenReturn(mcpServer);
        when(mcpComponentService.getMcpServerMcpComponents(1L)).thenReturn(List.of(new McpComponent()));

        List<McpServerFeatures.AsyncToolSpecification> toolSpecifications = buildToolSpecifications(Set.of());

        assertThat(toolSpecifications).isEmpty();
        verify(mcpComponentService, never()).getMcpServerMcpComponents(1L);
        verify(mcpProjectServiceTarget, never()).getMcpServerMcpProjects(1L);
        verify(workspaceMcpServerService, never()).fetchWorkspaceIdByMcpServerId(1L);
    }

    @Test
    void testDisabledMcpToolIsNotListed() {
        McpServer mcpServer = new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        mcpServer.setId(1L);

        McpComponent mcpComponent = new McpComponent();

        mcpComponent.setId(2L);

        McpTool enabledMcpTool = new McpTool("enabledTool", Map.of(), 2L);

        enabledMcpTool.setId(3L);

        McpTool disabledMcpTool = new McpTool("disabledTool", Map.of(), 2L);

        disabledMcpTool.setId(4L);
        disabledMcpTool.setEnabled(false);

        when(mcpServerServiceTarget.getMcpServer("secret")).thenReturn(mcpServer);
        when(mcpComponentService.getMcpServerMcpComponents(1L)).thenReturn(List.of(mcpComponent));
        when(mcpToolServiceTarget.getMcpComponentMcpTools(2L)).thenReturn(List.of(enabledMcpTool, disabledMcpTool));
        when(mcpProjectServiceTarget.getMcpServerMcpProjects(1L)).thenReturn(List.of());
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(1L)).thenReturn(Optional.empty());
        when(mcpToolFacade.getFunctionToolCallback(enabledMcpTool)).thenReturn(functionToolCallback("enabledTool"));

        List<McpServerFeatures.AsyncToolSpecification> toolSpecifications = buildToolSpecifications(Set.of());

        assertThat(toolSpecifications).hasSize(1);
        verify(mcpToolFacade, never()).getFunctionToolCallback(disabledMcpTool);
    }

    @Test
    void testWorkspaceToolProvidersReceiveTheServersWorkspaceId() {
        McpServer mcpServer = new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        mcpServer.setId(1L);

        McpServerWorkspaceToolCallbackContributor workspaceToolProvider =
            mock(McpServerWorkspaceToolCallbackContributor.class);

        when(mcpServerServiceTarget.getMcpServer("secret")).thenReturn(mcpServer);
        when(mcpComponentService.getMcpServerMcpComponents(1L)).thenReturn(List.of());
        when(mcpProjectServiceTarget.getMcpServerMcpProjects(1L)).thenReturn(List.of());
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(1L)).thenReturn(Optional.of(99L));
        when(workspaceToolProviders.orderedStream()).thenReturn(Stream.of(workspaceToolProvider));
        when(workspaceToolProvider.getFunctionToolCallbacks(99L))
            .thenReturn(List.of(functionToolCallback("workspaceTool")));

        List<McpServerFeatures.AsyncToolSpecification> toolSpecifications = buildToolSpecifications(Set.of());

        assertThat(toolSpecifications)
            .extracting(toolSpecification -> toolSpecification.tool()
                .name())
            .containsExactly("workspaceTool");
        verify(workspaceToolProvider).getFunctionToolCallbacks(99L);
    }

    @Test
    void testServerWithoutWorkspaceLinkGetsNoWorkspaceTools() {
        McpServer mcpServer = new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        mcpServer.setId(1L);

        McpServerWorkspaceToolCallbackContributor workspaceToolProvider =
            mock(McpServerWorkspaceToolCallbackContributor.class);

        when(mcpServerServiceTarget.getMcpServer("secret")).thenReturn(mcpServer);
        when(mcpComponentService.getMcpServerMcpComponents(1L)).thenReturn(List.of());
        when(mcpProjectServiceTarget.getMcpServerMcpProjects(1L)).thenReturn(List.of());
        when(workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(1L)).thenReturn(Optional.empty());
        when(workspaceToolProviders.orderedStream()).thenReturn(Stream.of(workspaceToolProvider));

        List<McpServerFeatures.AsyncToolSpecification> toolSpecifications = buildToolSpecifications(Set.of());

        assertThat(toolSpecifications).isEmpty();
        verify(workspaceToolProvider, never()).getFunctionToolCallbacks(any());
    }

    private List<McpServerFeatures.AsyncToolSpecification> buildToolSpecifications(Set<String> principalAuthorities) {
        return AutomationMcpServerConfiguration.buildToolSpecifications(
            "secret", principalAuthorities, mcpComponentService, mcpProjectService, mcpServerService, mcpToolService,
            mcpToolFacade, workspaceToolProviders, workspaceMcpServerService);
    }

    private static FunctionToolCallback<Map<String, Object>, Object> functionToolCallback(String toolName) {
        Function<Map<String, Object>, Object> toolFunction = request -> "ok";

        return FunctionToolCallback.builder(toolName, toolFunction)
            .inputType(Map.class)
            .inputSchema("{\"type\":\"object\"}")
            .build();
    }

    private static McpServer enforcingServer() {
        McpServer mcpServer = new McpServer("test-server", PlatformType.AUTOMATION, Environment.DEVELOPMENT);

        mcpServer.setEnabled(true);
        mcpServer.setEnforceToolAuthorization(true);

        return mcpServer;
    }

    private static McpComponent component(String componentName, Set<String> requiredAuthorities) {
        McpComponent mcpComponent = new McpComponent();

        mcpComponent.setComponentName(componentName);
        mcpComponent.setRequiredAuthorities(requiredAuthorities);

        return mcpComponent;
    }
}
