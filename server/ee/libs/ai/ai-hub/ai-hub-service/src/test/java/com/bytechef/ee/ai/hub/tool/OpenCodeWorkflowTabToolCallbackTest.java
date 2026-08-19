/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class OpenCodeWorkflowTabToolCallbackTest {

    @Test
    void testCallReturnsOpenedResult() {
        OpenCodeWorkflowTabToolCallback callback = new OpenCodeWorkflowTabToolCallback(null);

        String result = callback.call(
            "{\"projectId\":\"7\",\"language\":\"JAVASCRIPT\",\"name\":\"My Code Workflow\"}", null);

        assertThat(result).contains("\"opened\":true")
            .contains("\"projectId\":\"7\"")
            .contains("\"language\":\"JAVASCRIPT\"")
            .contains("My Code Workflow");
    }

    @Test
    void testCallRecordsCodeWorkflowReferenceWhenRecorderPresent() {
        AiHubChatArtifactRecorder artifactRecorder = mock(AiHubChatArtifactRecorder.class);

        OpenCodeWorkflowTabToolCallback callback = new OpenCodeWorkflowTabToolCallback(artifactRecorder);

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(7L, 42L, null, null, 0L, "thread-9").toToolContext());

        callback.call(
            "{\"projectId\":\"7\",\"language\":\"JAVASCRIPT\",\"name\":\"My Code Workflow\"}", toolContext);

        verify(artifactRecorder).recordReference(
            "thread-9", 42L, "CODE_WORKFLOW_REFERENCED", "7", "My Code Workflow");
    }

    @Test
    void testCallDoesNotRecordWhenRecorderAbsent() {
        OpenCodeWorkflowTabToolCallback callback = new OpenCodeWorkflowTabToolCallback(null);

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(7L, 42L, null, null, 0L, "thread-9").toToolContext());

        String result = callback.call(
            "{\"projectId\":\"7\",\"language\":\"JAVASCRIPT\",\"name\":\"My Code Workflow\"}", toolContext);

        assertThat(result).contains("\"opened\":true");
    }

    @Test
    void testCallReturnsToolErrorWhenProjectIdBlank() {
        OpenCodeWorkflowTabToolCallback callback = new OpenCodeWorkflowTabToolCallback(null);

        String result = callback.call(
            "{\"projectId\":\"\",\"language\":\"JAVASCRIPT\",\"name\":\"My Code Workflow\"}", null);

        assertThat(result).contains("projectId is required");
    }

    @Test
    void testCallReturnsToolErrorWhenLanguageBlank() {
        OpenCodeWorkflowTabToolCallback callback = new OpenCodeWorkflowTabToolCallback(null);

        String result = callback.call(
            "{\"projectId\":\"7\",\"language\":\"\",\"name\":\"My Code Workflow\"}", null);

        assertThat(result).contains("language is required");
    }

    @Test
    void testCallReturnsToolErrorWhenNameBlank() {
        OpenCodeWorkflowTabToolCallback callback = new OpenCodeWorkflowTabToolCallback(null);

        String result = callback.call(
            "{\"projectId\":\"7\",\"language\":\"JAVASCRIPT\",\"name\":\"\"}", null);

        assertThat(result).contains("name is required");
    }

    @Test
    void testToolDefinitionName() {
        assertThat(new OpenCodeWorkflowTabToolCallback(null).getToolDefinition()
            .name()).isEqualTo("openCodeWorkflowTab");
    }
}
