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

package com.bytechef.automation.workflow.execution.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkflowExecutionAgentTypeTest {

    @Test
    void testKeysAreUnique() {
        List<String> keys = Arrays.stream(WorkflowExecutionAgentType.values())
            .map(WorkflowExecutionAgentType::key)
            .toList();

        assertThat(keys).doesNotHaveDuplicates();
    }

    @Test
    void testNoWorkflowExecutionAgentTypeIsAFallback() {
        assertThat(WorkflowExecutionAgentType.values()).noneMatch(WorkflowExecutionAgentType::isFallback);
    }
}
