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

package com.bytechef.platform.tool.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * These enums are persisted by ordinal in {@code tool_invocation_log}. Ordinals are load-bearing — append new values at
 * the end only; never reorder or remove.
 *
 * @author Ivica Cardic
 */
class ToolExecutionEnumOrdinalStabilityTest {

    @Test
    void testSurfaceOrdinals() {
        assertThat(Arrays.stream(ToolExecutionSurface.values())
            .map(Enum::name)
            .toList())
                .containsExactly(
                    "MCP_AUTOMATION", "MCP_EMBEDDED", "MCP_MANAGEMENT", "EMBEDDED_API_ACTION", "EMBEDDED_API_TOOL");
    }

    @Test
    void testKindOrdinals() {
        assertThat(Arrays.stream(ToolExecutionKind.values())
            .map(Enum::name)
            .toList())
                .containsExactly("COMPONENT", "WORKFLOW", "CONTRIBUTED", "MANAGEMENT_TOOL");
    }

    @Test
    void testOutcomeOrdinals() {
        assertThat(Arrays.stream(ToolExecutionOutcome.values())
            .map(Enum::name)
            .toList())
                .containsExactly("SUCCESS", "ERROR", "CONNECTION_REQUIRED", "TIMEOUT");
    }
}
