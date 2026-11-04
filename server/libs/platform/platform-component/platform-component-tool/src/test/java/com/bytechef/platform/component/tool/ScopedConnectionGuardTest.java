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

package com.bytechef.platform.component.tool;

import static com.bytechef.platform.component.tool.ScopedConnectionTestFixtures.newGuard;
import static com.bytechef.platform.component.tool.ScopedConnectionTestFixtures.newGuardEntitledTo;
import static com.bytechef.platform.component.tool.ScopedConnectionTestFixtures.scopedToolContext;
import static com.bytechef.platform.component.tool.ScopedConnectionTestFixtures.workspaceToolContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.ai.copilot.connection.CopilotConnectionLister.CopilotConnection;
import com.bytechef.ai.copilot.connection.CopilotConnectionLister.CopilotConnectionRequest;
import com.bytechef.ai.copilot.connection.WorkspaceConnectionLister;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ai.copilot.tool.context.ScopedProjectToolContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ScopedConnectionGuardTest {

    @Test
    void testWorkspaceRunIsEntitledToAConnectionOfItsWorkspace() {
        WorkspaceConnectionLister workspaceConnectionLister = mock(WorkspaceConnectionLister.class);
        List<CopilotConnection> connections = List.of(new CopilotConnection(42L, "Slack", 0, true));

        when(workspaceConnectionLister.listConnections(argThat(ScopedConnectionGuardTest::isSlackRequestOfWorkspace3)))
            .thenReturn(connections);

        ScopedConnectionGuard guard = newGuard(List.of(), workspaceConnectionLister);

        assertThat(guard.isEntitled("slack", 42L, workspaceToolContext(3L))).isTrue();
        assertThat(guard.toolErrorIfNotEntitled("slack", 42L, workspaceToolContext(3L))).isNull();
    }

    @Test
    void testWorkspaceRunIsNotEntitledToAConnectionOfAnotherWorkspace() {
        WorkspaceConnectionLister workspaceConnectionLister = mock(WorkspaceConnectionLister.class);
        List<CopilotConnection> connections = List.of(new CopilotConnection(42L, "Slack", 0, true));

        when(workspaceConnectionLister.listConnections(argThat(ScopedConnectionGuardTest::isSlackRequestOfWorkspace3)))
            .thenReturn(connections);

        ScopedConnectionGuard guard = newGuard(List.of(), workspaceConnectionLister);

        assertThat(guard.isEntitled("slack", 900L, workspaceToolContext(3L))).isFalse();
        assertThat(guard.toolErrorIfNotEntitled("slack", 900L, workspaceToolContext(3L)))
            .contains(ScopedConnectionGuard.NOT_ENTITLED_MESSAGE);
    }

    @Test
    void testRunWithoutAWorkspaceCannotUseAConnection() {
        WorkspaceConnectionLister workspaceConnectionLister = mock(WorkspaceConnectionLister.class);

        ScopedConnectionGuard guard = newGuard(List.of(), workspaceConnectionLister);

        ToolContext toolContext = new ToolContext(
            AgentToolInvocationContext.builder()
                .userId(7L)
                .environmentId(0L)
                .build()
                .toToolContext());

        assertThat(guard.fetchDenialMessage("slack", 42L, toolContext))
            .isEqualTo(ScopedConnectionGuard.NO_WORKSPACE_MESSAGE);
        assertThat(guard.fetchDenialMessage("slack", 42L, new ToolContext(Map.of())))
            .isEqualTo(ScopedConnectionGuard.NO_WORKSPACE_MESSAGE);
        assertThat(guard.fetchDenialMessage("slack", 42L, null))
            .isEqualTo(ScopedConnectionGuard.NO_WORKSPACE_MESSAGE);

        verifyNoInteractions(workspaceConnectionLister);
    }

    @Test
    void testWorkspaceRunCannotVerifyAConnectionWhenTheListingFails() {
        WorkspaceConnectionLister workspaceConnectionLister = mock(WorkspaceConnectionLister.class);

        when(workspaceConnectionLister.listConnections(argThat(ScopedConnectionGuardTest::isSlackRequestOfWorkspace3)))
            .thenThrow(new IllegalStateException("lookup failed"));

        ScopedConnectionGuard guard = newGuard(List.of(), workspaceConnectionLister);

        assertThat(guard.fetchDenialMessage("slack", 42L, workspaceToolContext(3L)))
            .isEqualTo(ScopedConnectionGuard.UNVERIFIED_MESSAGE);
    }

    @Test
    void testWorkspaceRunCannotVerifyAConnectionWithoutAWorkspaceConnectionLister() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        ScopedConnectionGuard guard = new ScopedConnectionGuard(
            beanFactory.getBeanProvider(CopilotConnectionLister.class),
            beanFactory.getBeanProvider(WorkspaceConnectionLister.class));

        assertThat(guard.fetchDenialMessage("slack", 42L, workspaceToolContext(3L)))
            .isEqualTo(ScopedConnectionGuard.UNVERIFIED_MESSAGE);
    }

    @Test
    void testWorkspaceRunWithoutAConnectionIdIsNotRestricted() {
        WorkspaceConnectionLister workspaceConnectionLister = mock(WorkspaceConnectionLister.class);

        ScopedConnectionGuard guard = newGuard(List.of(), workspaceConnectionLister);

        assertThat(guard.isEntitled("slack", null, new ToolContext(Map.of()))).isTrue();

        verifyNoInteractions(workspaceConnectionLister);
    }

    @Test
    void testScopedRunIsEntitledToAConnectionItsUserCanUse() {
        ScopedConnectionGuard guard = newGuard(List.of(new ScopedConnectionTestFixtures.FixedLister(List.of(42L))));

        assertThat(guard.isEntitled("slack", 42L, scopedToolContext())).isTrue();
        assertThat(guard.toolErrorIfNotEntitled("slack", 42L, scopedToolContext())).isNull();
    }

    @Test
    void testScopedRunIsNotEntitledToAnotherUsersConnection() {
        ScopedConnectionGuard guard = newGuard(List.of(new ScopedConnectionTestFixtures.FixedLister(List.of(42L))));

        assertThat(guard.isEntitled("slack", 900L, scopedToolContext())).isFalse();
        assertThat(guard.toolErrorIfNotEntitled("slack", 900L, scopedToolContext()))
            .contains(ScopedConnectionGuard.NOT_ENTITLED_MESSAGE);
    }

    @Test
    void testScopedRunCannotVerifyAConnectionWhenNoListerServesTheCaller() {
        ScopedConnectionGuard guard = newGuard(List.of());

        assertThat(guard.isEntitled("slack", 42L, scopedToolContext())).isFalse();
        assertThat(guard.fetchDenialMessage("slack", 42L, scopedToolContext()))
            .isEqualTo(ScopedConnectionGuard.UNVERIFIED_MESSAGE);
    }

    @Test
    void testScopedRunCannotVerifyAConnectionWhenTheListerFails() {
        ScopedConnectionGuard guard = newGuard(List.of(new FailingLister()));

        assertThat(guard.isEntitled("slack", 42L, scopedToolContext())).isFalse();
        assertThat(guard.fetchDenialMessage("slack", 42L, scopedToolContext()))
            .isEqualTo(ScopedConnectionGuard.UNVERIFIED_MESSAGE);
    }

    @Test
    void testScopedRunWithoutAnInvocationContextCannotVerifyAConnection() {
        ScopedConnectionGuard guard = newGuardEntitledTo(List.of(42L));

        ToolContext toolContext = new ToolContext(
            Map.of(ScopedProjectToolContext.TOOL_CONTEXT_SCOPED_PROJECT_ID_KEY, 1114L));

        assertThat(guard.fetchDenialMessage("slack", 42L, toolContext))
            .isEqualTo(ScopedConnectionGuard.UNVERIFIED_MESSAGE);
    }

    @Test
    void testScopedRunWithoutAConnectionIdIsNotRestricted() {
        ScopedConnectionGuard guard = newGuard(List.of());

        assertThat(guard.isEntitled("slack", null, scopedToolContext())).isTrue();
    }

    private static boolean isSlackRequestOfWorkspace3(CopilotConnectionRequest request) {
        return Objects.equals(request.workspaceId(), 3L) && Objects.equals(request.componentName(), "slack");
    }

    private static final class FailingLister implements CopilotConnectionLister {

        @Override
        public boolean supports(CopilotConnectionRequest request) {
            return true;
        }

        @Override
        public List<CopilotConnection> listConnections(CopilotConnectionRequest request) {
            throw new IllegalStateException("lookup failed");
        }
    }
}
