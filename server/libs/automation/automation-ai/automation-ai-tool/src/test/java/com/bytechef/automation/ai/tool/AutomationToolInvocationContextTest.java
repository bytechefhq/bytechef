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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

class AutomationToolInvocationContextTest {

    @Test
    void testRehydratesAllFieldsFromToolContext() {
        ToolContext toolContext = new ToolContext(Map.of(
            AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 42L,
            AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 2L));

        AutomationToolInvocationContext context = AutomationToolInvocationContext.fromToolContext(toolContext);

        assertThat(context).isNotNull();
        assertThat(context.workspaceId()).isEqualTo(42L);
        assertThat(context.environmentId()).isEqualTo(2L);
    }

    @Test
    void testReturnsNullWhenNoScopeKeysPresent() {
        assertThat(AutomationToolInvocationContext.fromToolContext(new ToolContext(Map.of("unrelated", "x")))).isNull();
        assertThat(AutomationToolInvocationContext.fromToolContext(null)).isNull();
    }

    @Test
    void testResolveEnvironmentDefaultsToZeroWhenAbsent() {
        assertThat(AutomationToolInvocationContext.resolveEnvironmentOrDefault(null)).isZero();

        AutomationToolInvocationContext noEnvironment = new AutomationToolInvocationContext(1L, null);

        assertThat(AutomationToolInvocationContext.resolveEnvironmentOrDefault(noEnvironment)).isZero();
    }

    @Test
    void testResolveEnvironmentReturnsOrdinal() {
        AutomationToolInvocationContext context = new AutomationToolInvocationContext(1L, 3L);

        assertThat(AutomationToolInvocationContext.resolveEnvironmentOrDefault(context)).isEqualTo(3);
    }

    @Nested
    class ResolveWorkspaceIdTest {

        private final WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        @Test
        void testNullRequestReturnsContextWorkspace() {
            Long workspaceId = AutomationToolInvocationContext.resolveWorkspaceId(
                null, workspaceToolContext(7L), workspaceFacade);

            assertThat(workspaceId).isEqualTo(7L);

            verifyNoInteractions(workspaceFacade);
        }

        @Test
        void testNullRequestWithoutContextReturnsNull() {
            assertThat(AutomationToolInvocationContext.resolveWorkspaceId(null, null, workspaceFacade)).isNull();

            verifyNoInteractions(workspaceFacade);
        }

        @Test
        void testRequestMatchingContextWorkspaceIsAccepted() {
            Long workspaceId = AutomationToolInvocationContext.resolveWorkspaceId(
                7L, workspaceToolContext(7L), workspaceFacade);

            assertThat(workspaceId).isEqualTo(7L);

            verifyNoInteractions(workspaceFacade);
        }

        @Test
        void testRequestDifferingFromContextWorkspaceIsRejected() {
            ToolContext toolContext = workspaceToolContext(7L);

            assertThatThrownBy(
                () -> AutomationToolInvocationContext.resolveWorkspaceId(42L, toolContext, workspaceFacade))
                    .isInstanceOf(WorkspaceNotAccessibleException.class)
                    .hasMessage("Workspace 42 is not the workspace 7 this conversation is scoped to.");

            verifyNoInteractions(workspaceFacade);
        }

        @Test
        void testRequestForAccessibleWorkspaceWithoutContextIsAccepted() {
            when(workspaceFacade.getMyWorkspaces()).thenReturn(List.of(new Workspace(42L, "Workspace 42")));

            assertThat(AutomationToolInvocationContext.resolveWorkspaceId(42L, null, workspaceFacade))
                .isEqualTo(42L);
        }

        @Test
        void testRequestForInaccessibleWorkspaceWithoutContextIsRejected() {
            when(workspaceFacade.getMyWorkspaces()).thenReturn(List.of(new Workspace(42L, "Workspace 42")));

            assertThatThrownBy(() -> AutomationToolInvocationContext.resolveWorkspaceId(99L, null, workspaceFacade))
                .isInstanceOf(WorkspaceNotAccessibleException.class)
                .hasMessage("Workspace 99 is not accessible to the caller.");
        }

        @Test
        void testFailureLoadingWorkspacesIsNotReportedAsAccessDenied() {
            when(workspaceFacade.getMyWorkspaces()).thenThrow(new IllegalArgumentException("broken"));

            assertThatThrownBy(() -> AutomationToolInvocationContext.resolveWorkspaceId(42L, null, workspaceFacade))
                .isInstanceOf(IllegalArgumentException.class)
                .isNotInstanceOf(WorkspaceNotAccessibleException.class);
        }

        private static ToolContext workspaceToolContext(long workspaceId) {
            return new ToolContext(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, workspaceId));
        }
    }

    @Nested
    class ResolveEnvironmentIdTest {

        @Test
        void testRequestedEnvironmentWins() {
            ToolContext toolContext = environmentToolContext(2L);

            assertThat(AutomationToolInvocationContext.resolveEnvironmentId("staging", toolContext))
                .isEqualTo(Environment.STAGING.ordinal());
        }

        @Test
        void testUnknownRequestedEnvironmentIsRejected() {
            assertThatThrownBy(() -> AutomationToolInvocationContext.resolveEnvironmentId("NOPE", null))
                .isInstanceOf(UnknownEnvironmentException.class)
                .hasMessageContaining("Unknown environment 'NOPE'");
        }

        @Test
        void testContextEnvironmentIsUsedWhenNoneRequested() {
            assertThat(AutomationToolInvocationContext.resolveEnvironmentId(null, environmentToolContext(2L)))
                .isEqualTo(2L);
        }

        @Test
        void testNumericStringContextEnvironmentIsParsed() {
            assertThat(AutomationToolInvocationContext.resolveEnvironmentId(null, environmentToolContext("1")))
                .isEqualTo(1L);
        }

        @Test
        void testDefaultsToDevelopmentWhenContextCarriesNoEnvironment() {
            assertThat(AutomationToolInvocationContext.resolveEnvironmentId(null, null))
                .isEqualTo(Environment.DEVELOPMENT.ordinal());
            assertThat(AutomationToolInvocationContext.resolveEnvironmentId(null, new ToolContext(Map.of("x", "y"))))
                .isEqualTo(Environment.DEVELOPMENT.ordinal());
        }

        @Test
        void testMalformedContextEnvironmentIsRejectedInsteadOfDefaulting() {
            ToolContext toolContext = environmentToolContext("not-a-number");

            assertThatThrownBy(() -> AutomationToolInvocationContext.resolveEnvironmentId(null, toolContext))
                .isInstanceOf(UnknownEnvironmentException.class)
                .hasMessageContaining("not-a-number");
        }

        @Test
        void testOutOfRangeContextEnvironmentIsRejected() {
            ToolContext toolContext = environmentToolContext(9L);

            assertThatThrownBy(() -> AutomationToolInvocationContext.resolveEnvironmentId(null, toolContext))
                .isInstanceOf(UnknownEnvironmentException.class);
        }

        private static ToolContext environmentToolContext(Object environmentId) {
            return new ToolContext(
                Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, environmentId));
        }
    }
}
