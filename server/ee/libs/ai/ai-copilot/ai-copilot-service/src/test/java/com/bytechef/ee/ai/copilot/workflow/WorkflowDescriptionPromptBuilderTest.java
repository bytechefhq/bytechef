/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
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
