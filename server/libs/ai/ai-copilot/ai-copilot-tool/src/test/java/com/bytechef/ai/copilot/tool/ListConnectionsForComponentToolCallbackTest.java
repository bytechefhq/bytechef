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

package com.bytechef.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
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
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
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
            componentDefinitionService, connectionDefinitionService, mock(ToolStateVisibilityMetrics.class),
            List.of(new WorkspaceCopilotConnectionLister(workspaceConnectionFacade, resolver)));

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
        // getConnectionDefinition's Optional.orElseThrow() raises NoSuchElementException when the component exposes no
        // connection definition — the legitimate "no connections to list" signal.
        when(connectionDefinitionService.getConnectionDefinition("noConnectionComponent", 1))
            .thenThrow(new NoSuchElementException("no connection definition"));

        PropertyOptionsResolver resolver = new PropertyOptionsResolver(
            new SecurityContextRehydrator(mock(UserService.class), mock(AuthorityService.class)));

        ListConnectionsForComponentToolCallback callback = new ListConnectionsForComponentToolCallback(
            componentDefinitionService, connectionDefinitionService, mock(ToolStateVisibilityMetrics.class),
            List.of(new WorkspaceCopilotConnectionLister(mock(WorkspaceConnectionFacade.class), resolver)));

        ToolContext toolContext = new ToolContext(
            new AgentToolInvocationContext(1L, 10L, 0L, "thread-1", null).toToolContext());

        String result = callback.call("{\"componentName\":\"noConnectionComponent\"}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isFalse();
        assertThat(node.get("connections")
            .isEmpty()).isTrue();
    }

    @Test
    void testSurfacesFailureWhenConnectionDefinitionLookupFailsUnexpectedly() throws Exception {
        ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
        ConnectionDefinitionService connectionDefinitionService = mock(ConnectionDefinitionService.class);

        when(componentDefinitionService.fetchComponentDefinition("slack", null))
            .thenReturn(Optional.of(mock(ComponentDefinition.class)));
        // A non-NoSuchElementException failure is a real backend defect, not "no connections" — it must NOT be masked
        // as an empty envelope. The tool reports a failure to the LLM instead.
        when(connectionDefinitionService.getConnectionDefinition("slack", 1))
            .thenThrow(new IllegalStateException("registry unavailable"));

        PropertyOptionsResolver resolver = new PropertyOptionsResolver(
            new SecurityContextRehydrator(mock(UserService.class), mock(AuthorityService.class)));

        ListConnectionsForComponentToolCallback callback = new ListConnectionsForComponentToolCallback(
            componentDefinitionService, connectionDefinitionService, mock(ToolStateVisibilityMetrics.class),
            List.of(new WorkspaceCopilotConnectionLister(mock(WorkspaceConnectionFacade.class), resolver)));

        ToolContext toolContext = new ToolContext(
            new AgentToolInvocationContext(1L, 10L, 0L, "thread-1", null).toToolContext());

        String result = callback.call("{\"componentName\":\"slack\"}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        // The outer RuntimeException handler turns this into an error/lookup-failed envelope, not an empty connections
        // list — so the LLM does not falsely conclude the component has zero connections.
        assertThat(node.has("error")).isTrue();
        assertThat(node.has("connections")).isFalse();
    }

    @Test
    void testReturnsToolErrorWithSuggestionsWhenComponentAmbiguous() throws Exception {
        ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
        ComponentDefinition googleMail = mock(ComponentDefinition.class);
        ComponentDefinition googleDrive = mock(ComponentDefinition.class);

        when(googleMail.getName()).thenReturn("googleMail");
        when(googleMail.getTitle()).thenReturn("Gmail");
        when(googleDrive.getName()).thenReturn("googleDrive");
        when(googleDrive.getTitle()).thenReturn("Google Drive");

        // "google" matches two components, so there is no single unambiguous slug to auto-resolve to — the tool must
        // still fail loud with candidates so the agent picks one, rather than guessing.
        when(componentDefinitionService.fetchComponentDefinition("google", null)).thenReturn(Optional.empty());
        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(googleMail, googleDrive));

        PropertyOptionsResolver resolver = new PropertyOptionsResolver(
            new SecurityContextRehydrator(mock(UserService.class), mock(AuthorityService.class)));

        ListConnectionsForComponentToolCallback callback = new ListConnectionsForComponentToolCallback(
            componentDefinitionService, mock(ConnectionDefinitionService.class), mock(ToolStateVisibilityMetrics.class),
            List.of(new WorkspaceCopilotConnectionLister(mock(WorkspaceConnectionFacade.class), resolver)));

        ToolContext toolContext = new ToolContext(
            new AgentToolInvocationContext(1L, 10L, 0L, "thread-1", null).toToolContext());

        String result = callback.call("{\"componentName\":\"google\"}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();

        String error = node.get("error")
            .asString();

        assertThat(error).contains("google");
        assertThat(error).contains("googleMail (Gmail)");
        assertThat(error).contains("googleDrive (Google Drive)");
    }

    @Test
    void testAutoResolvesSingleFuzzyMatchAndListsConnections() throws Exception {
        assertAutoResolves("gmail");
    }

    @Test
    void testAutoResolvesHyphenatedNameAndListsConnections() throws Exception {
        // The colloquial "google-mail" used to match nothing (the hyphen broke the raw substring match), forcing an
        // extra round-trip; normalized matching now resolves it to the single googleMail slug.
        assertAutoResolves("google-mail");
    }

    private void assertAutoResolves(String requestedComponentName) throws Exception {
        ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
        ConnectionDefinitionService connectionDefinitionService = mock(ConnectionDefinitionService.class);
        WorkspaceConnectionFacade workspaceConnectionFacade = mock(WorkspaceConnectionFacade.class);
        UserService userService = mock(UserService.class);

        ComponentDefinition googleMail = mock(ComponentDefinition.class);

        when(googleMail.getName()).thenReturn("googleMail");
        when(googleMail.getTitle()).thenReturn("Gmail");

        when(componentDefinitionService.fetchComponentDefinition(requestedComponentName, null))
            .thenReturn(Optional.empty());
        when(componentDefinitionService.getComponentDefinitions()).thenReturn(List.of(googleMail));
        when(connectionDefinitionService.getConnectionDefinition("googleMail", 1))
            .thenReturn(buildConnectionDefinition(1));

        User user = mock(User.class);

        when(user.getLogin()).thenReturn("user@localhost.com");
        when(user.getAuthorityIds()).thenReturn(List.of());
        when(userService.fetchUser(10L)).thenReturn(Optional.of(user));

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .id(77L)
            .name("Gmail24")
            .componentName("googleMail")
            .connectionVersion(1)
            .environmentId(0)
            .active(true)
            .build();

        when(workspaceConnectionFacade.getConnections(eq(1L), eq("googleMail"), eq(1), eq(0L), eq(null)))
            .thenReturn(List.of(connectionDTO));

        PropertyOptionsResolver resolver =
            new PropertyOptionsResolver(new SecurityContextRehydrator(userService, mock(AuthorityService.class)));

        ListConnectionsForComponentToolCallback callback = new ListConnectionsForComponentToolCallback(
            componentDefinitionService, connectionDefinitionService, mock(ToolStateVisibilityMetrics.class),
            List.of(new WorkspaceCopilotConnectionLister(workspaceConnectionFacade, resolver)));

        ToolContext toolContext = new ToolContext(
            new AgentToolInvocationContext(1L, 10L, 0L, "thread-1", null).toToolContext());

        String result = callback.call("{\"componentName\":\"" + requestedComponentName + "\"}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isFalse();
        assertThat(node.get("componentName")
            .asText()).isEqualTo("googleMail");
        assertThat(node.get("resolvedFromComponentName")
            .asText()).isEqualTo(requestedComponentName);
        assertThat(node.get("connections")
            .size()).isEqualTo(1);
        assertThat(node.get("connections")
            .get(0)
            .get("id")
            .asLong()).isEqualTo(77L);
    }

    @Test
    void testReturnsToolErrorWhenWorkspaceContextMissing() throws Exception {
        PropertyOptionsResolver resolver = new PropertyOptionsResolver(
            new SecurityContextRehydrator(mock(UserService.class), mock(AuthorityService.class)));

        ListConnectionsForComponentToolCallback callback = new ListConnectionsForComponentToolCallback(
            mock(ComponentDefinitionService.class), mock(ConnectionDefinitionService.class),
            mock(ToolStateVisibilityMetrics.class),
            List.of(new WorkspaceCopilotConnectionLister(mock(WorkspaceConnectionFacade.class), resolver)));

        String result = callback.call("{\"componentName\":\"slack\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("Invocation context unavailable");
    }

    @Test
    void testRejectsBlankComponentName() throws Exception {
        PropertyOptionsResolver resolver = new PropertyOptionsResolver(
            new SecurityContextRehydrator(mock(UserService.class), mock(AuthorityService.class)));

        ListConnectionsForComponentToolCallback callback = new ListConnectionsForComponentToolCallback(
            mock(ComponentDefinitionService.class), mock(ConnectionDefinitionService.class),
            mock(ToolStateVisibilityMetrics.class),
            List.of(new WorkspaceCopilotConnectionLister(mock(WorkspaceConnectionFacade.class), resolver)));

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
