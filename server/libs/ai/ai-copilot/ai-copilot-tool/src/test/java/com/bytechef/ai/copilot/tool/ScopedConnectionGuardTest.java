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

import static com.bytechef.ai.copilot.tool.ScopedConnectionTestFixtures.newGuard;
import static com.bytechef.ai.copilot.tool.ScopedConnectionTestFixtures.newGuardEntitledTo;
import static com.bytechef.ai.copilot.tool.ScopedConnectionTestFixtures.scopedToolContext;
import static com.bytechef.ai.copilot.tool.ScopedConnectionTestFixtures.workspaceToolContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ai.copilot.tool.context.ScopedProjectToolContext;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ScopedConnectionGuardTest {

    @Test
    void testWorkspaceRunIsEntitledToAConnectionOfItsWorkspace() {
        WorkspaceConnectionFacade workspaceConnectionFacade = mock(WorkspaceConnectionFacade.class);
        List<ConnectionDTO> connectionDTOs = List.of(connectionDTO(42L));

        when(workspaceConnectionFacade.getConnections(3L, "slack", null, 0L, null))
            .thenReturn(connectionDTOs);

        ScopedConnectionGuard guard = newGuard(List.of(), workspaceConnectionFacade);

        assertThat(guard.isEntitled("slack", 42L, workspaceToolContext(3L))).isTrue();
        assertThat(guard.toolErrorIfNotEntitled("slack", 42L, workspaceToolContext(3L))).isNull();
    }

    @Test
    void testWorkspaceRunIsNotEntitledToAConnectionOfAnotherWorkspace() {
        WorkspaceConnectionFacade workspaceConnectionFacade = mock(WorkspaceConnectionFacade.class);
        List<ConnectionDTO> connectionDTOs = List.of(connectionDTO(42L));

        when(workspaceConnectionFacade.getConnections(3L, "slack", null, 0L, null))
            .thenReturn(connectionDTOs);

        ScopedConnectionGuard guard = newGuard(List.of(), workspaceConnectionFacade);

        assertThat(guard.isEntitled("slack", 900L, workspaceToolContext(3L))).isFalse();
        assertThat(guard.toolErrorIfNotEntitled("slack", 900L, workspaceToolContext(3L)))
            .contains(ScopedConnectionGuard.NOT_ENTITLED_MESSAGE);
    }

    @Test
    void testRunWithoutAWorkspaceCannotUseAConnection() {
        WorkspaceConnectionFacade workspaceConnectionFacade = mock(WorkspaceConnectionFacade.class);

        ScopedConnectionGuard guard = newGuard(List.of(), workspaceConnectionFacade);

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

        verifyNoInteractions(workspaceConnectionFacade);
    }

    @Test
    void testWorkspaceRunCannotVerifyAConnectionWhenTheListingFails() {
        WorkspaceConnectionFacade workspaceConnectionFacade = mock(WorkspaceConnectionFacade.class);

        when(workspaceConnectionFacade.getConnections(3L, "slack", null, 0L, null))
            .thenThrow(new IllegalStateException("lookup failed"));

        ScopedConnectionGuard guard = newGuard(List.of(), workspaceConnectionFacade);

        assertThat(guard.fetchDenialMessage("slack", 42L, workspaceToolContext(3L)))
            .isEqualTo(ScopedConnectionGuard.UNVERIFIED_MESSAGE);
    }

    @Test
    void testWorkspaceRunWithoutAConnectionIdIsNotRestricted() {
        WorkspaceConnectionFacade workspaceConnectionFacade = mock(WorkspaceConnectionFacade.class);

        ScopedConnectionGuard guard = newGuard(List.of(), workspaceConnectionFacade);

        assertThat(guard.isEntitled("slack", null, new ToolContext(Map.of()))).isTrue();

        verifyNoInteractions(workspaceConnectionFacade);
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

    private static ConnectionDTO connectionDTO(long id) {
        ConnectionDTO connectionDTO = mock(ConnectionDTO.class);

        when(connectionDTO.id()).thenReturn(id);

        return connectionDTO;
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
