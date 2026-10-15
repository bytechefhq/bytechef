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
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ai.copilot.tool.context.ScopedProjectToolContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * @author Ivica Cardic
 */
class ScopedConnectionGuardTest {

    @Test
    void testUnscopedRunIsEntitledToAnyConnection() {
        ScopedConnectionGuard guard = newGuard(List.of());

        assertThat(guard.isEntitled("slack", 900L, new ToolContext(Map.of()))).isTrue();
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
    void testRunThatSkipsAuthorizationWithoutAScopedProjectIsNotEntitledToAnyConnection() {
        ScopedConnectionGuard guard = newGuardEntitledTo(List.of(42L));

        ToolContext toolContext = new ToolContext(
            AgentToolInvocationContext.builder()
                .authentication(
                    new UsernamePasswordAuthenticationToken(
                        ScopedConnectionTestFixtures.EXTERNAL_USER_ID, null, List.of()))
                .skipAutomationAuthorization(true)
                .build()
                .toToolContext());

        assertThat(guard.fetchDenialMessage("slack", 42L, toolContext))
            .isEqualTo(ScopedConnectionGuard.NOT_ENTITLED_MESSAGE);
    }

    @Test
    void testScopedRunWithoutAConnectionIdIsNotRestricted() {
        ScopedConnectionGuard guard = newGuard(List.of());

        assertThat(guard.isEntitled("slack", null, scopedToolContext())).isTrue();
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
