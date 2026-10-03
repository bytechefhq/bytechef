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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.platform.component.definition.ActionContextAware;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class ToolSuspensionTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-10-04T00:00:00Z");

    @Test
    void testSuspendedToolResultIsRecordedInTheSuspendAndRecognized() {
        ActionContextAware actionContextAware = createSuspendedActionContext();

        String suspendedToolResult = ToolSuspension.suspendedToolResult(actionContextAware);

        Suspend suspend = captureSuspend(actionContextAware);

        Map<String, ?> continueParameters = suspend.continueParameters();

        assertThat(continueParameters.get("key")).isEqualTo("value");
        assertThat(suspend.expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(ToolSuspension.getSuspendedToolResult(suspend)).isEqualTo(suspendedToolResult);
        assertThat(ToolSuspension.isSuspendedToolResult(suspendedToolResult, suspend)).isTrue();
    }

    @Test
    void testJsonSerializedSuspendedToolResultIsRecognized() {
        ActionContextAware actionContextAware = createSuspendedActionContext();

        String suspendedToolResult = ToolSuspension.suspendedToolResult(actionContextAware);

        assertThat(ToolSuspension.isSuspendedToolResult("\"" + suspendedToolResult + "\"", suspendedToolResult))
            .isTrue();
    }

    @Test
    void testEachSuspensionGetsItsOwnSuspendedToolResult() {
        String firstSuspendedToolResult = ToolSuspension.suspendedToolResult(createSuspendedActionContext());
        String secondSuspendedToolResult = ToolSuspension.suspendedToolResult(createSuspendedActionContext());

        assertThat(firstSuspendedToolResult).isNotEqualTo(secondSuspendedToolResult);
        assertThat(ToolSuspension.isSuspendedToolResult(firstSuspendedToolResult, secondSuspendedToolResult))
            .isFalse();
    }

    @Test
    void testOtherToolResultsAreNotRecognized() {
        String suspendedToolResult = ToolSuspension.suspendedToolResult(createSuspendedActionContext());

        assertThat(ToolSuspension.isSuspendedToolResult(null, suspendedToolResult)).isFalse();
        assertThat(ToolSuspension.isSuspendedToolResult("", suspendedToolResult)).isFalse();
        assertThat(ToolSuspension.isSuspendedToolResult("{\"approved\":true}", suspendedToolResult)).isFalse();
        assertThat(ToolSuspension.isSuspendedToolResult("__bytechef_tool_suspended__", suspendedToolResult))
            .isFalse();
        assertThat(ToolSuspension.isSuspendedToolResult(suspendedToolResult, (String) null)).isFalse();
        assertThat(ToolSuspension.isSuspendedToolResult(suspendedToolResult, (Suspend) null)).isFalse();
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

    private static Suspend captureSuspend(ActionContextAware actionContextAware) {
        ArgumentCaptor<Suspend> suspendCaptor = ArgumentCaptor.forClass(Suspend.class);

        verify(actionContextAware).suspend(suspendCaptor.capture());

        return suspendCaptor.getValue();
    }

    private static ActionContextAware createSuspendedActionContext() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.getSuspend()).thenReturn(new Suspend(Map.of("key", "value"), EXPIRES_AT));

        return actionContextAware;
    }
}
