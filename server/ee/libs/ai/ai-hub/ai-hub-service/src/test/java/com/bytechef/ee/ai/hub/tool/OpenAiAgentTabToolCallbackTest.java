/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class OpenAiAgentTabToolCallbackTest {

    private static final ToolContext TOOL_CONTEXT = new ToolContext(
        new AiHubToolInvocationContext(7L, 42L, null, null, 0L, "thread-9").toToolContext());

    private final AiAgentFacade aiAgentFacade = mock(AiAgentFacade.class);
    private final AiHubChatArtifactRecorder artifactRecorder = mock(AiHubChatArtifactRecorder.class);
    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesOpenAiAgentTabName() {
        OpenAiAgentTabToolCallback openAiAgentTabToolCallback = new OpenAiAgentTabToolCallback(aiAgentFacade, null);

        assertThat(openAiAgentTabToolCallback.getToolDefinition()
            .name()).isEqualTo("openAiAgentTab");
    }

    @Test
    void testCallEchoesArgumentsAndRecordsTheAgentReference() {
        OpenAiAgentTabToolCallback openAiAgentTabToolCallback = new OpenAiAgentTabToolCallback(
            aiAgentFacade, artifactRecorder);

        JsonNode resultJsonNode = jsonMapper.readTree(
            openAiAgentTabToolCallback.call("{\"aiAgentId\":\"12\",\"name\":\"Support Agent\"}", TOOL_CONTEXT));

        assertThat(resultJsonNode.get("opened")
            .asBoolean()).isTrue();
        assertThat(resultJsonNode.get("aiAgentId")
            .asText()).isEqualTo("12");
        assertThat(resultJsonNode.get("name")
            .asText()).isEqualTo("Support Agent");

        verify(aiAgentFacade).getAgent(12L);
        verify(artifactRecorder).recordReference("thread-9", 42L, "AI_AGENT_REFERENCED", "12", "Support Agent");
    }

    @Test
    void testCallReturnsErrorAndRecordsNothingForUnknownAgent() {
        when(aiAgentFacade.getAgent(13L)).thenThrow(new IllegalArgumentException("AiAgent with id 13 not found"));

        assertErrorWithoutRecord("{\"aiAgentId\":\"13\",\"name\":\"Gone\"}");
    }

    @Test
    void testCallReturnsErrorAndRecordsNothingWhenAgentIsNotVisible() {
        when(aiAgentFacade.getAgent(14L)).thenThrow(new AccessDeniedException("denied"));

        assertErrorWithoutRecord("{\"aiAgentId\":\"14\",\"name\":\"Hidden\"}");
    }

    @Test
    void testCallReturnsErrorForNonNumericAgentId() {
        assertErrorWithoutRecord("{\"aiAgentId\":\"agent-42\",\"name\":\"Bad\"}");
    }

    @Test
    void testCallReturnsErrorWhenRequiredFieldsAreMissing() {
        assertErrorWithoutRecord("{\"name\":\"No Id\"}");
        assertErrorWithoutRecord("{\"aiAgentId\":\"12\"}");

        verifyNoInteractions(aiAgentFacade);
    }

    @Test
    void testCallReturnsErrorOnInvalidJson() {
        assertErrorWithoutRecord("not-json");
    }

    private void assertErrorWithoutRecord(String toolInput) {
        OpenAiAgentTabToolCallback openAiAgentTabToolCallback = new OpenAiAgentTabToolCallback(
            aiAgentFacade, artifactRecorder);

        JsonNode resultJsonNode = jsonMapper.readTree(openAiAgentTabToolCallback.call(toolInput, TOOL_CONTEXT));

        assertThat(resultJsonNode.has("error")).isTrue();
        assertThat(resultJsonNode.has("opened")).isFalse();

        verify(artifactRecorder, never()).recordReference(anyString(), any(), anyString(), anyString(), anyString());
    }
}
