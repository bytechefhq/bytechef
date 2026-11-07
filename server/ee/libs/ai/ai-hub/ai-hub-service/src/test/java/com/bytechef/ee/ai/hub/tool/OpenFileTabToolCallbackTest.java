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

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.exception.AssetFileNotFoundException;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class OpenFileTabToolCallbackTest {

    private static final ToolContext TOOL_CONTEXT = new ToolContext(
        new AiHubToolInvocationContext(7L, 42L, null, null, 0L, "thread-9").toToolContext());

    private final AiHubChatArtifactRecorder artifactRecorder = mock(AiHubChatArtifactRecorder.class);
    private final AssetFileFacade assetFileFacade = mock(AssetFileFacade.class);
    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesOpenFileTabName() {
        ToolDefinition toolDefinition = new OpenFileTabToolCallback(assetFileFacade, null).getToolDefinition();

        assertThat(toolDefinition.name()).isEqualTo("openFileTab");
        assertThat(toolDefinition.description()).isNotBlank();
        assertThat(toolDefinition.inputSchema()).contains("fileId", "name");
    }

    @Test
    void testCallEchoesArgumentsAndRecordsTheFileReference() {
        when(assetFileFacade.findByIdInWorkspace(12L, 7L)).thenReturn(new AssetFile());

        OpenFileTabToolCallback openFileTabToolCallback = new OpenFileTabToolCallback(
            assetFileFacade, artifactRecorder);

        JsonNode resultJsonNode = jsonMapper.readTree(
            openFileTabToolCallback.call("{\"fileId\":\"12\",\"name\":\"spec.md\"}", TOOL_CONTEXT));

        assertThat(resultJsonNode.get("opened")
            .asBoolean()).isTrue();
        assertThat(resultJsonNode.get("fileId")
            .asText()).isEqualTo("12");
        assertThat(resultJsonNode.get("name")
            .asText()).isEqualTo("spec.md");

        verify(artifactRecorder).recordReference("thread-9", 42L, "FILE_REFERENCED", "12", "spec.md");
    }

    @Test
    void testCallWithoutRecorderOpensWithoutRecording() {
        when(assetFileFacade.findByIdInWorkspace(12L, 7L)).thenReturn(new AssetFile());

        OpenFileTabToolCallback openFileTabToolCallback = new OpenFileTabToolCallback(assetFileFacade, null);

        JsonNode resultJsonNode = jsonMapper.readTree(
            openFileTabToolCallback.call("{\"fileId\":\"12\",\"name\":\"spec.md\"}", TOOL_CONTEXT));

        assertThat(resultJsonNode.get("opened")
            .asBoolean()).isTrue();

        verifyNoInteractions(artifactRecorder);
    }

    @Test
    void testCallReturnsErrorAndRecordsNothingForAFileOfAnotherWorkspace() {
        when(assetFileFacade.findByIdInWorkspace(13L, 7L))
            .thenThrow(new AssetFileNotFoundException("Asset file 13 not found in workspace 7"));

        assertErrorWithoutRecord("{\"fileId\":\"13\",\"name\":\"other.md\"}");
    }

    @Test
    void testCallReturnsErrorAndRecordsNothingWhenTheWorkspaceIsNotAccessible() {
        when(assetFileFacade.findByIdInWorkspace(14L, 7L)).thenThrow(new AccessDeniedException("denied"));

        assertErrorWithoutRecord("{\"fileId\":\"14\",\"name\":\"hidden.md\"}");
    }

    @Test
    void testCallReturnsErrorForNonNumericFileId() {
        assertErrorWithoutRecord("{\"fileId\":\"file-42\",\"name\":\"bad.md\"}");
    }

    @Test
    void testCallReturnsErrorWhenRequiredFieldsAreMissing() {
        assertErrorWithoutRecord("{\"name\":\"spec.md\"}");
        assertErrorWithoutRecord("{\"fileId\":\"12\"}");

        verifyNoInteractions(assetFileFacade);
    }

    @Test
    void testCallReturnsErrorOnInvalidJson() {
        assertErrorWithoutRecord("not-json");
    }

    private void assertErrorWithoutRecord(String toolInput) {
        OpenFileTabToolCallback openFileTabToolCallback = new OpenFileTabToolCallback(
            assetFileFacade, artifactRecorder);

        JsonNode resultJsonNode = jsonMapper.readTree(openFileTabToolCallback.call(toolInput, TOOL_CONTEXT));

        assertThat(resultJsonNode.has("error")).isTrue();
        assertThat(resultJsonNode.has("opened")).isFalse();

        verify(artifactRecorder, never()).recordReference(anyString(), any(), anyString(), anyString(), anyString());
    }
}
