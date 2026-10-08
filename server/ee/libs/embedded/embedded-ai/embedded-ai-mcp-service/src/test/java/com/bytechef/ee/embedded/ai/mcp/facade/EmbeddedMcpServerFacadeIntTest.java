/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringJUnitConfig(EmbeddedMcpServerFacadeIntTest.MethodSecurityConfiguration.class)
@TestPropertySource(properties = "bytechef.edition=ee")
class EmbeddedMcpServerFacadeIntTest {

    private static final long AUTOMATION_MCP_COMPONENT_ID = 21L;
    private static final long AUTOMATION_MCP_SERVER_ID = 11L;
    private static final long AUTOMATION_MCP_TOOL_ID = 31L;
    private static final long EMBEDDED_MCP_COMPONENT_ID = 20L;
    private static final long EMBEDDED_MCP_SERVER_ID = 10L;
    private static final long EMBEDDED_MCP_TOOL_ID = 30L;

    @MockitoBean
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private EmbeddedMcpServerFacade embeddedMcpServerFacade;

    @MockitoBean
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @MockitoBean
    private IntegrationService integrationService;

    @MockitoBean
    private McpComponentService mcpComponentService;

    @MockitoBean
    private McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;

    @MockitoBean
    private McpServerFacade mcpServerFacade;

    @MockitoBean
    private McpServerService mcpServerService;

    @MockitoBean
    private McpToolService mcpToolService;

    @MockitoBean
    private TagService tagService;

    @BeforeEach
    void beforeEach() {
        when(mcpServerService.getMcpServer(EMBEDDED_MCP_SERVER_ID))
            .thenReturn(createMcpServer(EMBEDDED_MCP_SERVER_ID, PlatformType.EMBEDDED));
        when(mcpServerService.getMcpServer(AUTOMATION_MCP_SERVER_ID))
            .thenReturn(createMcpServer(AUTOMATION_MCP_SERVER_ID, PlatformType.AUTOMATION));
        when(mcpComponentService.getMcpComponent(EMBEDDED_MCP_COMPONENT_ID))
            .thenReturn(createMcpComponent(EMBEDDED_MCP_COMPONENT_ID, EMBEDDED_MCP_SERVER_ID));
        when(mcpComponentService.getMcpComponent(AUTOMATION_MCP_COMPONENT_ID))
            .thenReturn(createMcpComponent(AUTOMATION_MCP_COMPONENT_ID, AUTOMATION_MCP_SERVER_ID));
        when(mcpToolService.fetchMcpTool(EMBEDDED_MCP_TOOL_ID))
            .thenReturn(Optional.of(createMcpTool(EMBEDDED_MCP_TOOL_ID, EMBEDDED_MCP_COMPONENT_ID)));
        when(mcpToolService.fetchMcpTool(AUTOMATION_MCP_TOOL_ID))
            .thenReturn(Optional.of(createMcpTool(AUTOMATION_MCP_TOOL_ID, AUTOMATION_MCP_COMPONENT_ID)));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testEveryMethodRequiresTenantAdmin() {
        authenticate(nonAdmin());

        List<Method> methods = Arrays.stream(EmbeddedMcpServerFacade.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> !method.isDefault())
            .toList();

        assertThat(methods).isNotEmpty();

        for (Method method : methods) {
            assertThatThrownBy(() -> invoke(method))
                .as("%s.%s", EmbeddedMcpServerFacade.class.getSimpleName(), method.getName())
                .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    class DeniedTest {

        @ParameterizedTest
        @MethodSource("com.bytechef.ee.embedded.ai.mcp.facade.EmbeddedMcpServerFacadeIntTest#administrationOperations")
        void testDeniedToNonAdmin(Consumer<EmbeddedMcpServerFacade> operation) {
            assertDenied(nonAdmin(), operation);
        }

        @ParameterizedTest
        @MethodSource("com.bytechef.ee.embedded.ai.mcp.facade.EmbeddedMcpServerFacadeIntTest#administrationOperations")
        void testDeniedToConnectedUserToken(Consumer<EmbeddedMcpServerFacade> operation) {
            assertDenied(connectedUser(false), operation);
        }

        @ParameterizedTest
        @MethodSource("com.bytechef.ee.embedded.ai.mcp.facade.EmbeddedMcpServerFacadeIntTest#administrationOperations")
        void testDeniedToEmbeddedApiKey(Consumer<EmbeddedMcpServerFacade> operation) {
            assertDenied(connectedUser(true), operation);
        }

        @ParameterizedTest
        @MethodSource("com.bytechef.ee.embedded.ai.mcp.facade.EmbeddedMcpServerFacadeIntTest#administrationOperations")
        void testDeniedToConnectedUserUnderSkipChecks(Consumer<EmbeddedMcpServerFacade> operation) {
            authenticate(connectedUser(false));

            assertThatThrownBy(() -> AutomationAuthorizationContext.callSkippingChecks(() -> {
                operation.accept(embeddedMcpServerFacade);

                return null;
            }))
                .isInstanceOf(AccessDeniedException.class);

            verifyNoServiceInteractions();
        }

        private void assertDenied(Authentication authentication, Consumer<EmbeddedMcpServerFacade> operation) {
            authenticate(authentication);

            assertThatThrownBy(() -> operation.accept(embeddedMcpServerFacade))
                .isInstanceOf(AccessDeniedException.class);

            verifyNoServiceInteractions();
        }

        private void verifyNoServiceInteractions() {
            verifyNoInteractions(
                componentDefinitionService, integrationInstanceConfigurationService, integrationService,
                mcpComponentService, mcpIntegrationInstanceToolService, mcpServerFacade, mcpServerService,
                mcpToolService, tagService);
        }
    }

    @Nested
    class TenantAdminTest {

        @BeforeEach
        void beforeEach() {
            authenticate(tenantAdmin());
        }

        @Test
        void testCreateEmbeddedMcpComponent() {
            McpComponent mcpComponent = createMcpComponent(null, EMBEDDED_MCP_SERVER_ID);
            List<McpTool> mcpTools = List.of(new McpTool("tool", Map.of()));

            when(mcpServerFacade.create(mcpComponent, mcpTools)).thenReturn(mcpComponent);

            assertThat(embeddedMcpServerFacade.createEmbeddedMcpComponent(mcpComponent, mcpTools))
                .isSameAs(mcpComponent);
        }

        @Test
        void testCreateEmbeddedMcpServerWithoutEnabled() {
            McpServer mcpServer = createMcpServer(EMBEDDED_MCP_SERVER_ID, PlatformType.EMBEDDED);

            when(mcpServerService.create("Embedded", PlatformType.EMBEDDED, Environment.DEVELOPMENT, null))
                .thenReturn(mcpServer);

            assertThat(embeddedMcpServerFacade.createEmbeddedMcpServer("Embedded", Environment.DEVELOPMENT, null))
                .isSameAs(mcpServer);
        }

        @Test
        void testDeleteEmbeddedMcpComponent() {
            embeddedMcpServerFacade.deleteEmbeddedMcpComponent(EMBEDDED_MCP_COMPONENT_ID);

            verify(mcpServerFacade).deleteMcpComponent(EMBEDDED_MCP_COMPONENT_ID);
        }

        @Test
        void testDeleteEmbeddedMcpServer() {
            embeddedMcpServerFacade.deleteEmbeddedMcpServer(EMBEDDED_MCP_SERVER_ID);

            verify(mcpServerFacade).deleteMcpServer(EMBEDDED_MCP_SERVER_ID);
        }

        @Test
        void testDeleteEmbeddedMcpTool() {
            embeddedMcpServerFacade.deleteEmbeddedMcpTool(EMBEDDED_MCP_TOOL_ID);

            verify(mcpToolService).delete(any(McpTool.class));
        }

        @Test
        void testGetEmbeddedMcpComponentMcpTools() {
            List<McpTool> mcpTools = List.of(createMcpTool(EMBEDDED_MCP_TOOL_ID, EMBEDDED_MCP_COMPONENT_ID));

            when(mcpToolService.getMcpComponentMcpTools(EMBEDDED_MCP_COMPONENT_ID)).thenReturn(mcpTools);

            assertThat(embeddedMcpServerFacade.getEmbeddedMcpComponentMcpTools(EMBEDDED_MCP_COMPONENT_ID))
                .isSameAs(mcpTools);
        }

        @Test
        void testGetEmbeddedMcpServerMcpComponents() {
            List<McpComponent> mcpComponents = List.of(
                createMcpComponent(EMBEDDED_MCP_COMPONENT_ID, EMBEDDED_MCP_SERVER_ID));

            when(mcpComponentService.getMcpServerMcpComponents(EMBEDDED_MCP_SERVER_ID)).thenReturn(mcpComponents);

            assertThat(embeddedMcpServerFacade.getEmbeddedMcpServerMcpComponents(EMBEDDED_MCP_SERVER_ID))
                .isSameAs(mcpComponents);
        }

        @Test
        void testUpdateEmbeddedMcpComponent() {
            McpComponent mcpComponent = createMcpComponent(EMBEDDED_MCP_COMPONENT_ID, EMBEDDED_MCP_SERVER_ID);
            List<McpTool> mcpTools = List.of(new McpTool("tool", Map.of()));

            when(mcpServerFacade.update(mcpComponent, mcpTools)).thenReturn(mcpComponent);

            assertThat(embeddedMcpServerFacade.updateEmbeddedMcpComponent(mcpComponent, mcpTools))
                .isSameAs(mcpComponent);
        }

        @Test
        void testUpdateEmbeddedMcpServer() {
            McpServer mcpServer = createMcpServer(EMBEDDED_MCP_SERVER_ID, PlatformType.EMBEDDED);

            when(mcpServerService.update(EMBEDDED_MCP_SERVER_ID, "Renamed", false)).thenReturn(mcpServer);

            assertThat(
                embeddedMcpServerFacade.updateEmbeddedMcpServer(EMBEDDED_MCP_SERVER_ID, "Renamed", false, null, null))
                    .isSameAs(mcpServer);

            verify(mcpServerService, never()).update(any(McpServer.class));
        }

        @Test
        void testUpdateEmbeddedMcpServerAuthenticationSwitches() {
            McpServer mcpServer = createMcpServer(EMBEDDED_MCP_SERVER_ID, PlatformType.EMBEDDED);

            when(mcpServerService.update(EMBEDDED_MCP_SERVER_ID, null, null)).thenReturn(mcpServer);
            when(mcpServerService.update(mcpServer)).thenReturn(mcpServer);

            embeddedMcpServerFacade.updateEmbeddedMcpServer(EMBEDDED_MCP_SERVER_ID, null, null, true, true);

            assertThat(mcpServer.isAuthenticationRequired()).isTrue();
            assertThat(mcpServer.isEnforceToolAuthorization()).isTrue();

            verify(mcpServerService).update(mcpServer);
        }

        @Test
        void testUpdateEmbeddedMcpServerSecretKey() {
            McpServer rotatedMcpServer = createMcpServer(EMBEDDED_MCP_SERVER_ID, PlatformType.EMBEDDED);

            when(mcpServerService.rotateSecretKey(EMBEDDED_MCP_SERVER_ID)).thenReturn(rotatedMcpServer);

            assertThat(embeddedMcpServerFacade.updateEmbeddedMcpServerSecretKey(EMBEDDED_MCP_SERVER_ID))
                .isSameAs(rotatedMcpServer);

            verify(mcpServerService).rotateSecretKey(EMBEDDED_MCP_SERVER_ID);
        }

        @Test
        void testUpdateEmbeddedMcpServerTags() {
            List<Tag> tags = List.of(new Tag("tag"));

            when(mcpServerFacade.updateMcpServerTags(EMBEDDED_MCP_SERVER_ID, tags)).thenReturn(tags);

            assertThat(embeddedMcpServerFacade.updateEmbeddedMcpServerTags(EMBEDDED_MCP_SERVER_ID, tags))
                .isSameAs(tags);
        }

        @Test
        void testUpdateEmbeddedMcpTool() {
            McpTool mcpTool = createMcpTool(EMBEDDED_MCP_TOOL_ID, EMBEDDED_MCP_COMPONENT_ID);

            when(mcpToolService.update(mcpTool)).thenReturn(mcpTool);

            assertThat(embeddedMcpServerFacade.updateEmbeddedMcpTool(mcpTool)).isSameAs(mcpTool);
        }

        @Test
        void testUpdateEmbeddedMcpToolEnabled() {
            embeddedMcpServerFacade.updateEmbeddedMcpToolEnabled(EMBEDDED_MCP_TOOL_ID, false);

            verify(mcpToolService).updateEnabled(EMBEDDED_MCP_TOOL_ID, false);
        }
    }

    @Nested
    class NonEmbeddedMcpServerTest {

        @BeforeEach
        void beforeEach() {
            authenticate(tenantAdmin());
        }

        @Test
        void testCreateEmbeddedMcpComponentRejectsAutomationMcpServer() {
            McpComponent mcpComponent = createMcpComponent(null, AUTOMATION_MCP_SERVER_ID);

            assertThatThrownBy(() -> embeddedMcpServerFacade.createEmbeddedMcpComponent(mcpComponent, List.of()))
                .isInstanceOf(IllegalArgumentException.class);

            verify(mcpServerFacade, never()).create(any(), any());
        }

        @Test
        void testDeleteEmbeddedMcpComponentRejectsAutomationMcpComponent() {
            assertThatThrownBy(() -> embeddedMcpServerFacade.deleteEmbeddedMcpComponent(AUTOMATION_MCP_COMPONENT_ID))
                .isInstanceOf(IllegalArgumentException.class);

            verify(mcpServerFacade, never()).deleteMcpComponent(anyLong());
        }

        @Test
        void testDeleteEmbeddedMcpServerRejectsAutomationMcpServer() {
            assertThatThrownBy(() -> embeddedMcpServerFacade.deleteEmbeddedMcpServer(AUTOMATION_MCP_SERVER_ID))
                .isInstanceOf(IllegalArgumentException.class);

            verify(mcpServerFacade, never()).deleteMcpServer(anyLong());
        }

        @Test
        void testDeleteEmbeddedMcpToolRejectsAutomationMcpTool() {
            assertThatThrownBy(() -> embeddedMcpServerFacade.deleteEmbeddedMcpTool(AUTOMATION_MCP_TOOL_ID))
                .isInstanceOf(IllegalArgumentException.class);

            verify(mcpToolService, never()).delete(any());
        }

        @Test
        void testGetEmbeddedMcpComponentMcpToolsRejectsAutomationMcpComponent() {
            assertThatThrownBy(
                () -> embeddedMcpServerFacade.getEmbeddedMcpComponentMcpTools(AUTOMATION_MCP_COMPONENT_ID))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(mcpToolService, never()).getMcpComponentMcpTools(anyLong());
        }

        @Test
        void testGetEmbeddedMcpServerMcpComponentsRejectsAutomationMcpServer() {
            assertThatThrownBy(
                () -> embeddedMcpServerFacade.getEmbeddedMcpServerMcpComponents(AUTOMATION_MCP_SERVER_ID))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(mcpComponentService, never()).getMcpServerMcpComponents(anyLong());
        }

        @Test
        void testUpdateEmbeddedMcpComponentRejectsMoveToAutomationMcpServer() {
            McpComponent mcpComponent = createMcpComponent(EMBEDDED_MCP_COMPONENT_ID, AUTOMATION_MCP_SERVER_ID);

            assertThatThrownBy(() -> embeddedMcpServerFacade.updateEmbeddedMcpComponent(mcpComponent, List.of()))
                .isInstanceOf(IllegalArgumentException.class);

            verify(mcpServerFacade, never()).update(any(), any());
        }

        @Test
        void testUpdateEmbeddedMcpServerRejectsAutomationMcpServer() {
            assertThatThrownBy(
                () -> embeddedMcpServerFacade.updateEmbeddedMcpServer(
                    AUTOMATION_MCP_SERVER_ID, "Renamed", true, null, null))
                        .isInstanceOf(IllegalArgumentException.class);

            verify(mcpServerService, never()).update(anyLong(), any(), any());
        }

        @Test
        void testUpdateEmbeddedMcpServerSecretKeyRejectsAutomationMcpServer() {
            assertThatThrownBy(
                () -> embeddedMcpServerFacade.updateEmbeddedMcpServerSecretKey(AUTOMATION_MCP_SERVER_ID))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(mcpServerService, never()).rotateSecretKey(anyLong());
        }

        @Test
        void testUpdateEmbeddedMcpServerTagsRejectsAutomationMcpServer() {
            assertThatThrownBy(
                () -> embeddedMcpServerFacade.updateEmbeddedMcpServerTags(AUTOMATION_MCP_SERVER_ID, List.of()))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(mcpServerFacade, never()).updateMcpServerTags(anyLong(), any());
        }

        @Test
        void testUpdateEmbeddedMcpToolRejectsMoveToAutomationMcpComponent() {
            McpTool mcpTool = createMcpTool(EMBEDDED_MCP_TOOL_ID, AUTOMATION_MCP_COMPONENT_ID);

            assertThatThrownBy(() -> embeddedMcpServerFacade.updateEmbeddedMcpTool(mcpTool))
                .isInstanceOf(IllegalArgumentException.class);

            verify(mcpToolService, never()).update(any());
        }

        @Test
        void testUpdateEmbeddedMcpToolEnabledRejectsAutomationMcpTool() {
            assertThatThrownBy(
                () -> embeddedMcpServerFacade.updateEmbeddedMcpToolEnabled(AUTOMATION_MCP_TOOL_ID, false))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(mcpToolService, never()).updateEnabled(anyLong(), anyBoolean());
        }
    }

    static Stream<Arguments> administrationOperations() {
        return Stream.of(
            operation(
                "createEmbeddedMcpComponent",
                facade -> facade.createEmbeddedMcpComponent(
                    createMcpComponent(null, EMBEDDED_MCP_SERVER_ID), List.of())),
            operation(
                "createEmbeddedMcpServer",
                facade -> facade.createEmbeddedMcpServer("Embedded", Environment.DEVELOPMENT, true)),
            operation(
                "deleteEmbeddedMcpComponent", facade -> facade.deleteEmbeddedMcpComponent(EMBEDDED_MCP_COMPONENT_ID)),
            operation("deleteEmbeddedMcpServer", facade -> facade.deleteEmbeddedMcpServer(EMBEDDED_MCP_SERVER_ID)),
            operation("deleteEmbeddedMcpTool", facade -> facade.deleteEmbeddedMcpTool(EMBEDDED_MCP_TOOL_ID)),
            operation(
                "getEmbeddedMcpComponentMcpTools",
                facade -> facade.getEmbeddedMcpComponentMcpTools(EMBEDDED_MCP_COMPONENT_ID)),
            operation(
                "getEmbeddedMcpServerMcpComponents",
                facade -> facade.getEmbeddedMcpServerMcpComponents(EMBEDDED_MCP_SERVER_ID)),
            operation("getEmbeddedMcpServers", EmbeddedMcpServerFacade::getEmbeddedMcpServers),
            operation("getEmbeddedMcpServerTags", EmbeddedMcpServerFacade::getEmbeddedMcpServerTags),
            operation("getMcpComponentDefinitions", EmbeddedMcpServerFacade::getMcpComponentDefinitions),
            operation(
                "updateEmbeddedMcpComponent",
                facade -> facade.updateEmbeddedMcpComponent(
                    createMcpComponent(EMBEDDED_MCP_COMPONENT_ID, EMBEDDED_MCP_SERVER_ID), List.of())),
            operation(
                "updateEmbeddedMcpServer",
                facade -> facade.updateEmbeddedMcpServer(EMBEDDED_MCP_SERVER_ID, "Renamed", false, null, null)),
            operation(
                "updateEmbeddedMcpServerSecretKey",
                facade -> facade.updateEmbeddedMcpServerSecretKey(EMBEDDED_MCP_SERVER_ID)),
            operation(
                "updateEmbeddedMcpServerTags",
                facade -> facade.updateEmbeddedMcpServerTags(EMBEDDED_MCP_SERVER_ID, List.of(new Tag("tag")))),
            operation(
                "updateEmbeddedMcpTool",
                facade -> facade.updateEmbeddedMcpTool(
                    createMcpTool(EMBEDDED_MCP_TOOL_ID, EMBEDDED_MCP_COMPONENT_ID))),
            operation(
                "updateEmbeddedMcpToolEnabled",
                facade -> facade.updateEmbeddedMcpToolEnabled(EMBEDDED_MCP_TOOL_ID, true)));
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }

    private static Authentication connectedUser(boolean apiKeyAuthenticated) {
        return new EmbeddedApiKeyAuthenticationToken(
            Environment.DEVELOPMENT.ordinal(), 1L, new User("external-user-1", "", List.of()), apiKeyAuthenticated);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }

        if (type == int.class) {
            return 0;
        }

        if (type == long.class) {
            return 0L;
        }

        return null;
    }

    private static McpComponent createMcpComponent(Long id, long mcpServerId) {
        McpComponent mcpComponent = new McpComponent("component", 1, mcpServerId, null);

        mcpComponent.setId(id);

        return mcpComponent;
    }

    private static McpServer createMcpServer(long id, PlatformType type) {
        McpServer mcpServer = new McpServer("Server", type, Environment.DEVELOPMENT, true);

        mcpServer.setId(id);

        return mcpServer;
    }

    private static McpTool createMcpTool(long id, long mcpComponentId) {
        McpTool mcpTool = new McpTool("tool", Map.of(), mcpComponentId);

        mcpTool.setId(id);

        return mcpTool;
    }

    private void invoke(Method method) throws Throwable {
        Object[] arguments = Arrays.stream(method.getParameterTypes())
            .map(EmbeddedMcpServerFacadeIntTest::defaultValue)
            .toArray();

        try {
            method.invoke(embeddedMcpServerFacade, arguments);
        } catch (InvocationTargetException invocationTargetException) {
            throw invocationTargetException.getCause();
        }
    }

    private static Authentication nonAdmin() {
        return new UsernamePasswordAuthenticationToken(
            "user", "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.USER)));
    }

    private static Arguments operation(String name, Consumer<EmbeddedMcpServerFacade> operation) {
        return Arguments.of(Named.of(name, operation));
    }

    private static Authentication tenantAdmin() {
        return new UsernamePasswordAuthenticationToken(
            "admin", "n/a",
            List.of(
                new SimpleGrantedAuthority(AuthorityConstants.ADMIN),
                new SimpleGrantedAuthority(AuthorityConstants.USER)));
    }

    @Configuration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import(EmbeddedMcpServerFacadeImpl.class)
    static class MethodSecurityConfiguration {

        @Bean("permissionService")
        PermissionService permissionService() {
            PermissionService permissionService = mock(PermissionService.class);

            when(permissionService.isTenantAdmin())
                .thenAnswer(invocation -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN));

            return permissionService;
        }
    }
}
