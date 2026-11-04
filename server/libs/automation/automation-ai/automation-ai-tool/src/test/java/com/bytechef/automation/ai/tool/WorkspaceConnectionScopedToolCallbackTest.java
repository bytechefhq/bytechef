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

package com.bytechef.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
class WorkspaceConnectionScopedToolCallbackTest {

    private static final long WORKSPACE_ID = 42L;
    private static final long OTHER_WORKSPACE_ID = 43L;
    private static final long DEVELOPMENT = 0L;
    private static final long PRODUCTION = 2L;
    private static final long OWNED_CONNECTION_ID = 7L;
    private static final long FOREIGN_CONNECTION_ID = 8L;
    private static final String DELEGATE_RESULT = "{\"options\":[]}";

    private final ToolCallback delegate = mock(ToolCallback.class);
    private final WorkspaceConnectionFacade workspaceConnectionFacade = mock(WorkspaceConnectionFacade.class);
    private final WorkspaceConnectionScopedToolCallback toolCallback = new WorkspaceConnectionScopedToolCallback(
        delegate, workspaceConnectionFacade);

    @BeforeEach
    void beforeEach() {
        when(delegate.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name("lookupPropertyOptions")
                .description("Lookup")
                .inputSchema("{\"type\":\"object\",\"properties\":{\"connectionId\":{\"type\":\"integer\"}}}")
                .build());
        when(delegate.call(anyString(), any(ToolContext.class))).thenReturn(DELEGATE_RESULT);

        when(workspaceConnectionFacade.getConnections(
            eq(WORKSPACE_ID), eq("slack"), isNull(), eq(DEVELOPMENT), isNull()))
                .thenReturn(List.of(connection(OWNED_CONNECTION_ID)));
        when(workspaceConnectionFacade.getConnections(
            eq(OTHER_WORKSPACE_ID), eq("slack"), isNull(), eq(DEVELOPMENT), isNull()))
                .thenReturn(List.of(connection(FOREIGN_CONNECTION_ID)));
        when(workspaceConnectionFacade.getConnections(
            eq(WORKSPACE_ID), eq("slack"), isNull(), eq(PRODUCTION), isNull()))
                .thenReturn(List.of());
        when(workspaceConnectionFacade.getConnections(
            eq(WORKSPACE_ID), eq("github"), isNull(), eq(DEVELOPMENT), isNull()))
                .thenReturn(List.of());
    }

    @Test
    void testToolDefinitionIsTheDelegates() {
        assertThat(toolCallback.getToolDefinition()).isSameAs(delegate.getToolDefinition());
    }

    @Test
    void testCallWithoutConnectionReachesDelegateWithoutOwnershipCheck() {
        String input = "{\"componentName\":\"slack\"}";
        ToolContext toolContext = forwardedContext(WORKSPACE_ID, DEVELOPMENT);

        assertThat(toolCallback.call(input, toolContext)).isEqualTo(DELEGATE_RESULT);

        verify(delegate).call(input, toolContext);
        verifyNoInteractions(workspaceConnectionFacade);
    }

    @Test
    void testCallWithNullConnectionReachesDelegate() {
        String input = "{\"componentName\":\"slack\",\"connectionId\":null}";

        assertThat(toolCallback.call(input, forwardedContext(WORKSPACE_ID, DEVELOPMENT))).isEqualTo(DELEGATE_RESULT);

        verifyNoInteractions(workspaceConnectionFacade);
    }

    @Test
    void testCallWithOwnedConnectionReachesDelegate() {
        String input = "{\"componentName\":\"slack\",\"connectionId\":" + OWNED_CONNECTION_ID + "}";
        ToolContext toolContext = forwardedContext(WORKSPACE_ID, DEVELOPMENT);

        assertThat(toolCallback.call(input, toolContext)).isEqualTo(DELEGATE_RESULT);

        verify(delegate).call(input, toolContext);
    }

    @Test
    void testCallWithForeignWorkspaceConnectionIsRefused() {
        String result = toolCallback.call(
            "{\"componentName\":\"slack\",\"connectionId\":" + FOREIGN_CONNECTION_ID + "}",
            forwardedContext(WORKSPACE_ID, DEVELOPMENT));

        assertRefused(result, FOREIGN_CONNECTION_ID);
    }

    @Test
    void testCallWithForeignWorkspaceConnectionAsStringIsRefused() {
        String result = toolCallback.call(
            "{\"componentName\":\"slack\",\"connectionId\":\"" + FOREIGN_CONNECTION_ID + "\"}",
            forwardedContext(WORKSPACE_ID, DEVELOPMENT));

        assertRefused(result, FOREIGN_CONNECTION_ID);
    }

    @Test
    void testCallWithConnectionAndWithoutComponentNameIsRefused() {
        String result = toolCallback.call(
            "{\"connectionId\":" + OWNED_CONNECTION_ID + "}", forwardedContext(WORKSPACE_ID, DEVELOPMENT));

        assertThat(result).contains("\"error\"")
            .contains("componentName is required");

        verify(delegate, never()).call(anyString(), nullable(ToolContext.class));
        verifyNoInteractions(workspaceConnectionFacade);
    }

    @Test
    void testCallWithConnectionAndBlankComponentNameIsRefused() {
        String result = toolCallback.call(
            "{\"componentName\":\" \",\"connectionId\":" + OWNED_CONNECTION_ID + "}",
            forwardedContext(WORKSPACE_ID, DEVELOPMENT));

        assertThat(result).contains("componentName is required");

        verify(delegate, never()).call(anyString(), nullable(ToolContext.class));
        verifyNoInteractions(workspaceConnectionFacade);
    }

    @Test
    void testCallWithConnectionFromOtherEnvironmentIsRefused() {
        String result = toolCallback.call(
            "{\"componentName\":\"slack\",\"connectionId\":" + OWNED_CONNECTION_ID + "}",
            forwardedContext(WORKSPACE_ID, PRODUCTION));

        assertRefused(result, OWNED_CONNECTION_ID);
    }

    @Test
    void testCallWithConnectionOfOtherComponentIsRefused() {
        String result = toolCallback.call(
            "{\"componentName\":\"github\",\"connectionId\":" + OWNED_CONNECTION_ID + "}",
            forwardedContext(WORKSPACE_ID, DEVELOPMENT));

        assertRefused(result, OWNED_CONNECTION_ID);
    }

    @Test
    void testCallWithoutForwardedWorkspaceIsRefused() {
        String result = toolCallback.call(
            "{\"componentName\":\"slack\",\"connectionId\":" + OWNED_CONNECTION_ID + "}",
            new ToolContext(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, DEVELOPMENT)));

        assertThat(result).contains("\"error\"")
            .contains("workspaceId");

        verify(delegate, never()).call(anyString(), nullable(ToolContext.class));
        verifyNoInteractions(workspaceConnectionFacade);
    }

    @Test
    void testCallWithoutToolContextIsRefused() {
        String result = toolCallback.call(
            "{\"componentName\":\"slack\",\"connectionId\":" + OWNED_CONNECTION_ID + "}");

        assertThat(result).contains("\"error\"");

        verify(delegate, never()).call(anyString(), nullable(ToolContext.class));
        verifyNoInteractions(workspaceConnectionFacade);
    }

    @Test
    void testCallWithNonIntegerConnectionIdIsRefused() {
        String result = toolCallback.call(
            "{\"componentName\":\"slack\",\"connectionId\":\"abc\"}", forwardedContext(WORKSPACE_ID, DEVELOPMENT));

        assertThat(result).contains("connectionId must be an integer");

        verify(delegate, never()).call(anyString(), nullable(ToolContext.class));
    }

    @Test
    void testCallWithMalformedInputIsRefusedWithoutEchoingTheParserText() {
        String result = toolCallback.call(
            "{\"componentName\":\"slack\",\"connectionId\": secret-token}",
            forwardedContext(WORKSPACE_ID, DEVELOPMENT));

        assertThat(result).contains("Invalid input: not valid JSON")
            .doesNotContain("secret-token");

        verify(delegate, never()).call(anyString(), nullable(ToolContext.class));
    }

    @Test
    void testCallWhenOwnershipLookupFailsDoesNotReachDelegate() {
        when(workspaceConnectionFacade.getConnections(anyLong(), eq("boom"), isNull(), anyLong(), isNull()))
            .thenThrow(new IllegalStateException("database down"));

        String result = toolCallback.call(
            "{\"componentName\":\"boom\",\"connectionId\":" + OWNED_CONNECTION_ID + "}",
            forwardedContext(WORKSPACE_ID, DEVELOPMENT));

        assertThat(result).contains("\"error\"");

        verify(delegate, never()).call(anyString(), nullable(ToolContext.class));
    }

    private void assertRefused(String result, long connectionId) {
        assertThat(result).contains("\"error\"")
            .contains("Connection " + connectionId)
            .contains("listConnections");

        verify(delegate, never()).call(anyString(), nullable(ToolContext.class));
    }

    private static ConnectionDTO connection(long id) {
        return ConnectionDTO.builder()
            .id(id)
            .componentName("slack")
            .build();
    }

    private static ToolContext forwardedContext(long workspaceId, long environmentId) {
        return new ToolContext(AutomationToolInvocationContext.toForwardedContext(null, workspaceId, environmentId));
    }
}
