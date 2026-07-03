/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.assetfile.domain.AssetFileFormat;
import com.bytechef.ee.ai.hub.artifact.ArtifactGeneratorRegistry;
import com.bytechef.ee.ai.hub.artifact.GenerationRequest;
import com.bytechef.ee.ai.hub.artifact.GenerationResult;
import com.bytechef.ee.ai.hub.task.AiHubTask;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class GenerateArtifactToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    private static ToolContext toolContext() {
        return new ToolContext(
            new AiHubToolInvocationContext(1L, 10L, (short) 0, "summarise", 0L, "thread-1").toToolContext());
    }

    @Test
    void testCallReturnsErrorWhenWorkspaceContextMissing() {
        // Probe-oracle defense: a tool input with no bound workspace must NOT default-author into workspace 0
        // — the chat surface is the only authoritative source of workspace identity. Pin the error path.
        ArtifactGeneratorRegistry registry = mock(ArtifactGeneratorRegistry.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);

        GenerateArtifactToolCallback callback = new GenerateArtifactToolCallback(registry, taskService);

        String result = callback.call(
            "{\"format\":\"MARKDOWN\",\"filename\":\"r.md\",\"content\":\"# x\"}");

        assertThat(result).contains("Workspace context unavailable");
        verify(registry, never()).generate(any(), any());
    }

    @Test
    void testCallDispatchesToRegistryWithBundledRequest() throws Exception {
        ArtifactGeneratorRegistry registry = mock(ArtifactGeneratorRegistry.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);

        AiHubTask task = mock(AiHubTask.class);

        when(task.getId()).thenReturn(7L);
        when(taskService.findByThreadId("thread-1")).thenReturn(Optional.of(task));

        when(registry.generate(eq(AssetFileFormat.MARKDOWN), any(GenerationRequest.class)))
            .thenReturn(new GenerationResult(42L, "report.md", AssetFileFormat.MARKDOWN, true));

        GenerateArtifactToolCallback callback = new GenerateArtifactToolCallback(registry, taskService);

        String result = callback.call(
            "{\"format\":\"MARKDOWN\",\"filename\":\"report.md\",\"content\":\"# hello\"}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("assetFileId")
            .asLong()).isEqualTo(42L);
        assertThat(node.get("filename")
            .asText()).isEqualTo("report.md");
        assertThat(node.get("format")
            .asText()).isEqualTo("MARKDOWN");
        assertThat(node.get("taskLinked")
            .asBoolean()).isTrue();

        ArgumentCaptor<GenerationRequest> requestCaptor = ArgumentCaptor.forClass(GenerationRequest.class);

        verify(registry).generate(eq(AssetFileFormat.MARKDOWN), requestCaptor.capture());

        GenerationRequest captured = requestCaptor.getValue();

        assertThat(captured.workspaceId()).isEqualTo(1L);
        assertThat(captured.userId()).isEqualTo(10L);
        assertThat(captured.environmentId()).isEqualTo(0);
        assertThat(captured.taskId()).isEqualTo(7L);
        assertThat(captured.generatedFromPrompt()).isEqualTo("summarise");
    }

    @Test
    void testCallTreatsMissingTaskAsFirstTurn() {
        // First-turn race: the tool fires before the chat endpoint has created the task row. The generator
        // must persist the file with taskId=null rather than failing — pin the request.taskId=null
        // path so a regression that throws on missing task surfaces here.
        ArtifactGeneratorRegistry registry = mock(ArtifactGeneratorRegistry.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);

        when(taskService.findByThreadId("thread-1")).thenReturn(Optional.empty());

        when(registry.generate(eq(AssetFileFormat.MARKDOWN), any(GenerationRequest.class)))
            .thenReturn(new GenerationResult(42L, "first.md", AssetFileFormat.MARKDOWN, false));

        GenerateArtifactToolCallback callback = new GenerateArtifactToolCallback(registry, taskService);

        callback.call(
            "{\"format\":\"MARKDOWN\",\"filename\":\"first.md\",\"content\":\"# x\"}",
            toolContext());

        ArgumentCaptor<GenerationRequest> requestCaptor = ArgumentCaptor.forClass(GenerationRequest.class);

        verify(registry).generate(eq(AssetFileFormat.MARKDOWN), requestCaptor.capture());

        assertThat(requestCaptor.getValue()
            .taskId()).isNull();
    }

    @Test
    void testCallReturnsErrorOnUnknownFormat() {
        ArtifactGeneratorRegistry registry = mock(ArtifactGeneratorRegistry.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);

        GenerateArtifactToolCallback callback = new GenerateArtifactToolCallback(registry, taskService);

        String result = callback.call(
            "{\"format\":\"BOGUS\",\"filename\":\"x\",\"content\":\"x\"}", toolContext());

        assertThat(result).contains("Unknown format 'BOGUS'");
        verify(registry, never()).generate(any(), any());
    }

    @Test
    void testCallReturnsErrorOnMissingFilename() {
        ArtifactGeneratorRegistry registry = mock(ArtifactGeneratorRegistry.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);

        GenerateArtifactToolCallback callback = new GenerateArtifactToolCallback(registry, taskService);

        String result = callback.call(
            "{\"format\":\"MARKDOWN\",\"filename\":\"\",\"content\":\"# x\"}", toolContext());

        assertThat(result).contains("filename is required");
    }

    @Test
    void testCallReturnsErrorWhenNoGeneratorRegisteredForFormat() {
        // The schema's enum advertises every format, but a deployment may not have registered all generators (e.g.
        // CE without the EE-only POI generators). Pin: the registry's IllegalArgumentException surfaces as a
        // structured tool error rather than bubbling up as a runtime failure.
        ArtifactGeneratorRegistry registry = mock(ArtifactGeneratorRegistry.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);

        when(registry.generate(eq(AssetFileFormat.PPTX), any(GenerationRequest.class)))
            .thenThrow(new IllegalArgumentException("No ArtifactGenerator registered for format 'PPTX'"));

        GenerateArtifactToolCallback callback = new GenerateArtifactToolCallback(registry, taskService);

        String result = callback.call(
            "{\"format\":\"PPTX\",\"filename\":\"deck.pptx\",\"content\":\"{}\"}", toolContext());

        assertThat(result).contains("No ArtifactGenerator registered for format 'PPTX'");
    }

    @Test
    void testCallDispatchesHtmlFormatToRegistry() throws Exception {
        ArtifactGeneratorRegistry registry = mock(ArtifactGeneratorRegistry.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);

        when(registry.generate(eq(AssetFileFormat.HTML), any(GenerationRequest.class)))
            .thenReturn(new GenerationResult(99L, "widget.html", AssetFileFormat.HTML, false));

        GenerateArtifactToolCallback callback = new GenerateArtifactToolCallback(registry, taskService);

        String html = "<!doctype html><html><head></head><body><h1>x</h1></body></html>";

        String result = callback.call(
            "{\"format\":\"HTML\",\"filename\":\"widget.html\",\"content\":" + jsonMapper.writeValueAsString(html)
                + "}",
            toolContext());

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.get("format")
            .asText()).isEqualTo("HTML");
        verify(registry).generate(eq(AssetFileFormat.HTML), any(GenerationRequest.class));
    }

    @Test
    void testToolDescriptionAdvertisesHtmlInSchemaEnum() {
        ArtifactGeneratorRegistry registry = mock(ArtifactGeneratorRegistry.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);

        GenerateArtifactToolCallback callback = new GenerateArtifactToolCallback(registry, taskService);

        String schema = callback.getToolDefinition()
            .inputSchema();

        assertThat(schema).contains("\"HTML\"");
    }

    @Test
    void testToolDescriptionContainsHtmlSteeringLine() {
        // The steering line is the only nudge keeping the LLM from reaching for HTML when CHART/MARKDOWN
        // would do. Pin it so a refactor of the description doesn't silently drop the guidance.
        ArtifactGeneratorRegistry registry = mock(ArtifactGeneratorRegistry.class);
        AiHubTaskService taskService = mock(AiHubTaskService.class);

        GenerateArtifactToolCallback callback = new GenerateArtifactToolCallback(registry, taskService);

        String description = callback.getToolDefinition()
            .description();

        assertThat(description)
            .contains("CHART for data visualization")
            .contains("HTML only for interactive apps");
    }
}
