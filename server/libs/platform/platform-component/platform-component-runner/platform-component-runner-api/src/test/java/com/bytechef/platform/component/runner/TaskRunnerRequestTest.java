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

package com.bytechef.platform.component.runner;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class TaskRunnerRequestTest {

    @Test
    public void testRejectsNeitherScriptNorCommands() {
        assertThatThrownBy(() -> newRequest(null, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("either script or commands");
    }

    @Test
    public void testRejectsBothScriptAndCommands() {
        assertThatThrownBy(() -> newRequest("return null;", List.of("echo hi")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("mutually exclusive");
    }

    private static TaskRunnerRequest newRequest(String script, List<String> commands) {
        return new TaskRunnerRequest(
            "js", script, commands, Map.of(), Map.of(), Map.of(), List.of(), null, null, Duration.ofMinutes(5),
            Map.of(), null);
    }
}
