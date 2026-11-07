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

import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.facade.CustomComponentFacade;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 * @author Ivica Cardic
 */
class OpenCustomComponentTabToolCallbackTest {

    private static final ToolContext TOOL_CONTEXT = new ToolContext(
        new AiHubToolInvocationContext(7L, 42L, null, null, 0L, "thread-9").toToolContext());

    private final AiHubChatArtifactRecorder artifactRecorder = mock(AiHubChatArtifactRecorder.class);
    private final CustomComponentFacade customComponentFacade = mock(CustomComponentFacade.class);
    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionName() {
        OpenCustomComponentTabToolCallback callback = new OpenCustomComponentTabToolCallback(
            customComponentFacade, null);

        assertThat(callback.getToolDefinition()
            .name()).isEqualTo("openCustomComponentTab");
    }

    @Test
    void testCallOpensAndRecordsAVisibleCustomComponent() {
        when(customComponentFacade.getCustomComponent(7L)).thenReturn(new CustomComponent());

        OpenCustomComponentTabToolCallback callback = new OpenCustomComponentTabToolCallback(
            customComponentFacade, artifactRecorder);

        JsonNode resultJsonNode = jsonMapper.readTree(
            callback.call("{\"customComponentId\":\"7\",\"name\":\"My Component\"}", TOOL_CONTEXT));

        assertThat(resultJsonNode.get("opened")
            .asBoolean()).isTrue();
        assertThat(resultJsonNode.get("customComponentId")
            .asText()).isEqualTo("7");
        assertThat(resultJsonNode.get("name")
            .asText()).isEqualTo("My Component");

        verify(customComponentFacade).getCustomComponent(7L);
        verify(artifactRecorder).recordReference("thread-9", 42L, "CUSTOM_COMPONENT_REFERENCED", "7", "My Component");
    }

    @Test
    void testCallOpensAVisibleCustomComponentWithoutARecorder() {
        when(customComponentFacade.getCustomComponent(7L)).thenReturn(new CustomComponent());

        OpenCustomComponentTabToolCallback callback = new OpenCustomComponentTabToolCallback(
            customComponentFacade, null);

        JsonNode resultJsonNode = jsonMapper.readTree(
            callback.call("{\"customComponentId\":\"7\",\"name\":\"My Component\"}", TOOL_CONTEXT));

        assertThat(resultJsonNode.get("opened")
            .asBoolean()).isTrue();
    }

    @Test
    void testCallReturnsErrorAndRecordsNothingWhenTheCallerMayNotSeeTheCustomComponent() {
        when(customComponentFacade.getCustomComponent(8L)).thenThrow(new AccessDeniedException("denied"));

        assertErrorWithoutRecord("{\"customComponentId\":\"8\",\"name\":\"Hidden\"}");
    }

    @Test
    void testCallReturnsErrorAndRecordsNothingForAnUnknownCustomComponent() {
        when(customComponentFacade.getCustomComponent(9L)).thenThrow(new NoSuchElementException("No value present"));

        assertErrorWithoutRecord("{\"customComponentId\":\"9\",\"name\":\"Gone\"}");
    }

    @Test
    void testCallReturnsErrorForANonNumericCustomComponentId() {
        assertErrorWithoutRecord("{\"customComponentId\":\"component-7\",\"name\":\"Bad\"}");

        verifyNoInteractions(customComponentFacade);
    }

    @Test
    void testCallReturnsErrorWhenRequiredFieldsAreMissing() {
        assertErrorWithoutRecord("{\"customComponentId\":\"\",\"name\":\"My Component\"}");
        assertErrorWithoutRecord("{\"customComponentId\":\"7\"}");

        verifyNoInteractions(customComponentFacade);
    }

    private void assertErrorWithoutRecord(String toolInput) {
        OpenCustomComponentTabToolCallback callback = new OpenCustomComponentTabToolCallback(
            customComponentFacade, artifactRecorder);

        JsonNode resultJsonNode = jsonMapper.readTree(callback.call(toolInput, TOOL_CONTEXT));

        assertThat(resultJsonNode.has("error")).isTrue();
        assertThat(resultJsonNode.has("opened")).isFalse();

        verify(artifactRecorder, never()).recordReference(anyString(), any(), anyString(), anyString(), anyString());
    }
}
