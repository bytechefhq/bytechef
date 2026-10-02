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

package com.bytechef.platform.ai.guardrails;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

/**
 * @author Ivica Cardic
 */
class GuardrailAdvisorOrderTest {

    @Test
    void testTheWorkspaceFloorIsHighestPrecedence() {
        assertThat(GuardrailAdvisorOrder.WORKSPACE_FLOOR).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    void testTheNodeCheckSitsExactlyOneInsideTheFloor() {
        assertThat(GuardrailAdvisorOrder.NODE_CHECK).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
        assertThat(GuardrailAdvisorOrder.NODE_CHECK).isGreaterThan(GuardrailAdvisorOrder.WORKSPACE_FLOOR);
    }
}
