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
class OpenCustomComponentTabToolCallbackTest {

    @Test
    void testCallReturnsOpenedResult() {
        OpenCustomComponentTabToolCallback callback = new OpenCustomComponentTabToolCallback(null);

        String result = callback.call("{\"customComponentId\":\"7\",\"name\":\"My Component\"}", null);

        assertThat(result).contains("\"opened\":true")
            .contains("\"customComponentId\":\"7\"")
            .contains("My Component");
    }

    @Test
    void testCallRecordsCustomComponentReferenceWhenRecorderPresent() {
        AiHubTaskArtifactRecorder artifactRecorder = mock(AiHubTaskArtifactRecorder.class);

        OpenCustomComponentTabToolCallback callback = new OpenCustomComponentTabToolCallback(artifactRecorder);

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(7L, 42L, null, null, 0L, "thread-9").toToolContext());

        callback.call("{\"customComponentId\":\"7\",\"name\":\"My Component\"}", toolContext);

        verify(artifactRecorder).recordReference("thread-9", 42L, "CUSTOM_COMPONENT_REFERENCED", "7", "My Component");
    }

    @Test
    void testCallReturnsToolErrorWhenCustomComponentIdBlank() {
        OpenCustomComponentTabToolCallback callback = new OpenCustomComponentTabToolCallback(null);

        String result = callback.call("{\"customComponentId\":\"\",\"name\":\"My Component\"}", null);

        assertThat(result).contains("customComponentId is required");
    }

    @Test
    void testToolDefinitionName() {
        assertThat(new OpenCustomComponentTabToolCallback(null).getToolDefinition()
            .name()).isEqualTo("openCustomComponentTab");
    }
}
