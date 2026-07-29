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

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

class AutomationToolInvocationContextTest {

    @Test
    void testRehydratesAllFieldsFromToolContext() {
        ToolContext toolContext = new ToolContext(Map.of(
            AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 42L,
            AutomationToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY, 7L,
            AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 2L,
            AutomationToolInvocationContext.TOOL_CONTEXT_THREAD_ID_KEY, "thread-1"));

        AutomationToolInvocationContext context = AutomationToolInvocationContext.fromToolContext(toolContext);

        assertThat(context).isNotNull();
        assertThat(context.workspaceId()).isEqualTo(42L);
        assertThat(context.userId()).isEqualTo(7L);
        assertThat(context.environmentId()).isEqualTo(2L);
        assertThat(context.threadId()).isEqualTo("thread-1");
    }

    @Test
    void testReturnsNullWhenNoScopeKeysPresent() {
        assertThat(AutomationToolInvocationContext.fromToolContext(new ToolContext(Map.of("unrelated", "x")))).isNull();
        assertThat(AutomationToolInvocationContext.fromToolContext(null)).isNull();
    }

    @Test
    void testResolveEnvironmentDefaultsToZeroWhenAbsent() {
        assertThat(AutomationToolInvocationContext.resolveEnvironmentOrDefault(null)).isZero();

        AutomationToolInvocationContext noEnvironment = new AutomationToolInvocationContext(
            1L, null, null, null, null, null);

        assertThat(AutomationToolInvocationContext.resolveEnvironmentOrDefault(noEnvironment)).isZero();
    }

    @Test
    void testResolveEnvironmentReturnsOrdinal() {
        AutomationToolInvocationContext context = new AutomationToolInvocationContext(1L, null, null, null, 3L, null);

        assertThat(AutomationToolInvocationContext.resolveEnvironmentOrDefault(context)).isEqualTo(3);
    }
}
