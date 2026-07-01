/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.ee.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ListConnectionsForComponentToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testListsConnectionsForComponent() throws Exception {
        ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
        ConnectionDefinitionService connectionDefinitionService = mock(ConnectionDefinitionService.class);
        WorkspaceConnectionFacade workspaceConnectionFacade = mock(WorkspaceConnectionFacade.class);
        UserService userService = mock(UserService.class);

        when(componentDefinitionService.fetchComponentDefinition("slack", null))
            .thenReturn(Optional.of(mock(ComponentDefinition.class)));
        when(connectionDefinitionService.getConnectionDefinition("slack", 1))
            .thenReturn(buildConnectionDefinition(2));

        User user = mock(User.class);

        when(user.getLogin()).thenReturn("user@localhost.com");
        when(user.getAuthorityIds()).thenReturn(List.of());
        when(userService.fetchUser(10L)).thenReturn(Optional.of(user));

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .id(42L)
            .name("Slack Prod")
            .componentName("slack")
            .connectionVersion(2)
            .environmentId(0)
            .active(true)
            .build();

        when(workspaceConnectionFacade.getConnections(eq(1L), eq("slack"), eq(2), eq(0L), eq(null)))
            .thenReturn(List.of(connectionDTO));

        PropertyOptionsResolver resolver =
            new PropertyOptionsResolver(new SecurityContextRehydrator(userService, mock(AuthorityService.class)));

        ListConnectionsForComponentToolCallback callback = new ListConnectionsForComponentToolCallback(
            componentDefinitionService, connectionDefinitionService, workspaceConnectionFacade, resolver,
            mock(ToolStateVisibilityMetrics.class));

        ToolContext toolContext = new ToolContext(
            new AgentToolInvocationContext(1L, 10L, 0L, "thread-1", null).toToolContext());

        String result = callback.call("{\"componentName\":\"slack\"}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("componentName")
            .asText()).isEqualTo("slack");
        assertThat(node.get("connectionVersion")
            .asInt()).isEqualTo(2);
        assertThat(node.get("connections")
            .size()).isEqualTo(1);
        assertThat(node.get("connections")
            .get(0)
            .get("id")
            .asLong()).isEqualTo(42L);
        assertThat(node.get("connections")
            .get(0)
            .get("active")
            .asBoolean()).isTrue();
    }

    @Test
    void testReturnsEmptyEnvelopeWhenComponentExistsButHasNoConnectionDefinition() throws Exception {
        ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
        ConnectionDefinitionService connectionDefinitionService = mock(ConnectionDefinitionService.class);

        when(componentDefinitionService.fetchComponentDefinition("noConnectionComponent", null))
            .thenReturn(Optional.of(mock(ComponentDefinition.class)));
        when(connectionDefinitionService.getConnectionDefinition("noConnectionComponent", 1))
            .thenThrow(new RuntimeException("no connection definition"));

        PropertyOptionsResolver resolver = new PropertyOptionsResolver(
            new SecurityContextRehydrator(mock(UserService.class), mock(AuthorityService.class)));

        ListConnectionsForComponentToolCallback callback = new ListConnectionsForComponentToolCallback(
            componentDefinitionService, connectionDefinitionService, mock(WorkspaceConnectionFacade.class), resolver,
            mock(ToolStateVisibilityMetrics.class));

        ToolContext toolContext = new ToolContext(
            new AgentToolInvocationContext(1L, 10L, 0L, "thread-1", null).toToolContext());

        String result = callback.call("{\"componentName\":\"noConnectionComponent\"}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isFalse();
        assertThat(node.get("connections")
            .isEmpty()).isTrue();
    }

    @Test
    void testReturnsToolErrorWithSuggestionsWhenComponentUnknown() throws Exception {
        ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
        ComponentDefinition googleMail = mock(ComponentDefinition.class);

        when(googleMail.getName()).thenReturn("googleMail");
        when(googleMail.getTitle()).thenReturn("Gmail");

        when(componentDefinitionService.fetchComponentDefinition("gmail", null)).thenReturn(Optional.empty());
        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(googleMail));

        PropertyOptionsResolver resolver = new PropertyOptionsResolver(
            new SecurityContextRehydrator(mock(UserService.class), mock(AuthorityService.class)));

        ListConnectionsForComponentToolCallback callback = new ListConnectionsForComponentToolCallback(
            componentDefinitionService, mock(ConnectionDefinitionService.class), mock(WorkspaceConnectionFacade.class),
            resolver, mock(ToolStateVisibilityMetrics.class));

        ToolContext toolContext = new ToolContext(
            new AgentToolInvocationContext(1L, 10L, 0L, "thread-1", null).toToolContext());

        String result = callback.call("{\"componentName\":\"gmail\"}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();

        String error = node.get("error")
            .asString();

        assertThat(error).contains("gmail");
        assertThat(error).contains("googleMail (Gmail)");
    }

    @Test
    void testReturnsToolErrorWhenWorkspaceContextMissing() throws Exception {
        PropertyOptionsResolver resolver = new PropertyOptionsResolver(
            new SecurityContextRehydrator(mock(UserService.class), mock(AuthorityService.class)));

        ListConnectionsForComponentToolCallback callback = new ListConnectionsForComponentToolCallback(
            mock(ComponentDefinitionService.class), mock(ConnectionDefinitionService.class),
            mock(WorkspaceConnectionFacade.class), resolver, mock(ToolStateVisibilityMetrics.class));

        String result = callback.call("{\"componentName\":\"slack\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("Workspace context unavailable");
    }

    @Test
    void testRejectsBlankComponentName() throws Exception {
        PropertyOptionsResolver resolver = new PropertyOptionsResolver(
            new SecurityContextRehydrator(mock(UserService.class), mock(AuthorityService.class)));

        ListConnectionsForComponentToolCallback callback = new ListConnectionsForComponentToolCallback(
            mock(ComponentDefinitionService.class), mock(ConnectionDefinitionService.class),
            mock(WorkspaceConnectionFacade.class), resolver, mock(ToolStateVisibilityMetrics.class));

        ToolContext toolContext = new ToolContext(
            new AgentToolInvocationContext(1L, 10L, 0L, "thread-1", null).toToolContext());

        String result = callback.call("{\"componentName\":\"\"}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asString()).contains("componentName is required");
    }

    private static ConnectionDefinition buildConnectionDefinition(int version) {
        try {
            // Bypass the SDK-level constructor and set the version field directly. Same workaround as the
            // discovery-callback tests use for ComponentDefinition / ActionDefinition.
            Constructor<?>[] constructors = ConnectionDefinition.class.getDeclaredConstructors();

            Constructor<?> noArgs = null;

            for (Constructor<?> candidate : constructors) {
                if (candidate.getParameterCount() == 0) {
                    noArgs = candidate;

                    break;
                }
            }

            if (noArgs == null) {
                throw new AssertionError("ConnectionDefinition has no no-arg constructor available");
            }

            noArgs.setAccessible(true);

            ConnectionDefinition connectionDefinition = (ConnectionDefinition) noArgs.newInstance();

            Field versionField = ConnectionDefinition.class.getDeclaredField("version");

            versionField.setAccessible(true);
            versionField.setInt(connectionDefinition, version);

            return connectionDefinition;
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to build ConnectionDefinition fixture", exception);
        }
    }
}
