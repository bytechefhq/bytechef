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
class OpenSkillTabToolCallbackTest {

    @Test
    void testCallReturnsOpenedResult() {
        OpenSkillTabToolCallback callback = new OpenSkillTabToolCallback(null);

        String result = callback.call("{\"skillId\":\"7\",\"name\":\"Triage\"}", null);

        assertThat(result).contains("\"opened\":true")
            .contains("\"skillId\":\"7\"")
            .contains("Triage");
    }

    @Test
    void testCallRecordsSkillReferenceWhenRecorderPresent() {
        AiHubChatArtifactRecorder artifactRecorder = mock(AiHubChatArtifactRecorder.class);

        OpenSkillTabToolCallback callback = new OpenSkillTabToolCallback(artifactRecorder);

        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(7L, 42L, null, null, 0L, "thread-9").toToolContext());

        callback.call("{\"skillId\":\"7\",\"name\":\"Triage\"}", toolContext);

        verify(artifactRecorder).recordReference("thread-9", 42L, "SKILL_REFERENCED", "7", "Triage");
    }

    @Test
    void testCallReturnsToolErrorWhenSkillIdBlank() {
        OpenSkillTabToolCallback callback = new OpenSkillTabToolCallback(null);

        String result = callback.call("{\"skillId\":\"\",\"name\":\"Triage\"}", null);

        assertThat(result).contains("skillId is required");
    }

    @Test
    void testToolDefinitionName() {
        assertThat(new OpenSkillTabToolCallback(null).getToolDefinition()
            .name()).isEqualTo("openSkillTab");
    }
}
