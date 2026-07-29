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

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class OpenResourceTabToolCallbackTest {

    private AiHubTaskArtifactRecorder artifactRecorder;
    private OpenResourceTabToolCallback toolCallback;

    @BeforeEach
    void beforeEach() {
        artifactRecorder = mock(AiHubTaskArtifactRecorder.class);
        toolCallback = new OpenResourceTabToolCallback(artifactRecorder);
    }

    @Test
    void testToolDefinitionName() {
        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("openResourceTab");
    }

    @Test
    void testMissingTypeReturnsError() {
        String result = toolCallback.call("{\"name\": \"Orders\"}");

        assertThat(result).contains("error");
        assertThat(result).contains("type is required");
    }

    @Test
    void testUnknownTypeReturnsError() {
        String result = toolCallback.call("{\"type\": \"WIDGET\", \"name\": \"Orders\"}");

        assertThat(result).contains("error");
        assertThat(result).contains("Unknown type");
    }

    @Test
    void testFileOutputMatchesLegacyShape() {
        String result = toolCallback.call("{\"type\": \"FILE\", \"name\": \"report.md\", \"fileId\": \"f-1\"}");

        assertThat(result).contains("\"opened\":true");
        assertThat(result).contains("\"type\":\"FILE\"");
        assertThat(result).contains("\"fileId\":\"f-1\"");
        assertThat(result).contains("\"name\":\"report.md\"");
    }

    @Test
    void testDataTableMissingIdReturnsError() {
        String result = toolCallback.call("{\"type\": \"DATA_TABLE\", \"name\": \"Orders\"}");

        assertThat(result).contains("error");
        assertThat(result).contains("dataTableId is required");
    }

    @Test
    void testWorkflowOutputCarriesAllThreeIdsAndRecordsArtifact() {
        ToolContext toolContext = new ToolContext(
            Map.of(
                AiHubToolInvocationContext.TOOL_CONTEXT_THREAD_ID_KEY, "thread-1",
                AiHubToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY, 9L));

        String result = toolCallback.call(
            "{\"type\": \"WORKFLOW\", \"name\": \"Sync\", \"workflowId\": \"w-1\", \"projectId\": \"12\", " +
                "\"projectWorkflowId\": 34}",
            toolContext);

        assertThat(result).contains("\"opened\":true");
        assertThat(result).contains("\"workflowId\":\"w-1\"");
        assertThat(result).contains("\"projectId\":\"12\"");
        assertThat(result).contains("\"projectWorkflowId\":34");

        verify(artifactRecorder).recordWorkflowReference("thread-1", 9L, "w-1", 12L, 34L, "Sync");
    }

    @Test
    void testCodeWorkflowRequiresLanguage() {
        String result = toolCallback.call("{\"type\": \"CODE_WORKFLOW\", \"name\": \"Script\", \"projectId\": \"5\"}");

        assertThat(result).contains("error");
        assertThat(result).contains("language is required");
    }

    @Test
    void testKnowledgeBaseRecordsReference() {
        ToolContext toolContext = new ToolContext(
            Map.of(
                AiHubToolInvocationContext.TOOL_CONTEXT_THREAD_ID_KEY, "thread-1",
                AiHubToolInvocationContext.TOOL_CONTEXT_USER_ID_KEY, 9L));

        toolCallback.call(
            "{\"type\": \"KNOWLEDGE_BASE\", \"name\": \"Docs\", \"knowledgeBaseId\": \"kb-1\"}", toolContext);

        verify(artifactRecorder).recordReference("thread-1", 9L, "KB_REFERENCED", "kb-1", "Docs");
    }
}
