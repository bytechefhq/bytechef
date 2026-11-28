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

package com.bytechef.ai.copilot.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkflowDescriptionPromptBuilderTest {

    private final WorkflowDescriptionPromptBuilder promptBuilder = new WorkflowDescriptionPromptBuilder();

    @Test
    void testBuildWholeWorkflowPrompt() {
        String prompt = promptBuilder.build("{\"label\":\"Sync\",\"tasks\":[]}", null);

        assertThat(prompt).contains("Sync");
        assertThat(prompt).contains("workflow");
        assertThat(prompt).doesNotContain("single step");
    }

    @Test
    void testBuildNodePrompt() {
        String prompt = promptBuilder.build("{\"tasks\":[{\"name\":\"node1\"}]}", "node1");

        assertThat(prompt).contains("node1");
        assertThat(prompt).contains("single step");
    }
}
