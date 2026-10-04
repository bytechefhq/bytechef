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

package com.bytechef.platform.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.definition.ActionContextAware;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ToolSuspensionTest {

    @Test
    void testSuspendedToolResultIsRecognized() {
        String suspendedToolResult = ToolSuspension.suspendedToolResult(createSuspendedActionContext());

        assertThat(ToolSuspension.isSuspendedToolResult(suspendedToolResult)).isTrue();
    }

    @Test
    void testJsonSerializedSuspendedToolResultIsRecognized() {
        String suspendedToolResult = ToolSuspension.suspendedToolResult(createSuspendedActionContext());

        assertThat(ToolSuspension.isSuspendedToolResult("\"" + suspendedToolResult + "\"")).isTrue();
    }

    @Test
    void testOtherToolResultsAreNotRecognized() {
        assertThat(ToolSuspension.isSuspendedToolResult(null)).isFalse();
        assertThat(ToolSuspension.isSuspendedToolResult("")).isFalse();
        assertThat(ToolSuspension.isSuspendedToolResult("{\"approved\":true}")).isFalse();
    }

    @Test
    void testSuspendedToolResultRequiresASuspend() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        assertThatThrownBy(() -> ToolSuspension.suspendedToolResult(actionContextAware))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testSuspendedToolResultRequiresTheAgentActionContext() {
        ActionContext actionContext = mock(ActionContext.class);

        assertThatThrownBy(() -> ToolSuspension.suspendedToolResult(actionContext))
            .isInstanceOf(IllegalStateException.class);
    }

    private static ActionContextAware createSuspendedActionContext() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.getSuspend()).thenReturn(new ActionContext.Suspend(Map.of(), null));

        return actionContextAware;
    }
}
