/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.agent.ChatModelUnavailableException;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.ai.copilot.tool.catalog.SubAgentModelRequest;
import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class DataAnalystToolCallbackTest {

    private final AiHubChatArtifactRecorder artifactRecorder = mock(AiHubChatArtifactRecorder.class);
    private final AssetFileFacade assetFileFacade = mock(AssetFileFacade.class);
    private final ChatClient chatClient = mock(ChatClient.class);
    private final List<ToolCallback> dataTableToolCallbacks = List.of(mock(ToolCallback.class));
    private final JsonMapper jsonMapper = new JsonMapper();
    private final ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
    private final CallResponseSpec responseSpec = mock(CallResponseSpec.class);

    @Test
    void testCallReturnsAShortReportInline() {
        stubSubagent("# Totals\n\nRevenue is 42.");

        JsonNode node = jsonMapper.readTree(newCallback().call("{\"question\":\"total revenue?\"}", toolContext()));

        assertThat(node.get("report")
            .asString()).isEqualTo("# Totals\n\nRevenue is 42.");

        verifyNoInteractions(assetFileFacade);
    }

    @Test
    void testCallHandsTheSubagentTheDataTableToolsTheParentContextAndTheTableId() {
        stubSubagent("# Report");

        ToolContext toolContext = toolContext();

        newCallback().call("{\"question\":\"orders by region\",\"dataTableId\":\"12\"}", toolContext);

        verify(chatClient).prompt("orders by region\n\nFocus on data table id: 12");
        verify(requestSpec).tools(dataTableToolCallbacks);
        verify(requestSpec).toolContext(toolContext.getContext());
    }

    @Test
    void testCallSavesALongReportToAFileAndRecordsTheArtifact() {
        String report = "x".repeat(DataAnalystToolCallback.INLINE_REPORT_MAX_LENGTH + 1);

        AssetFile assetFile = new AssetFile();

        assetFile.setId(99L);
        assetFile.setName("data-analyst-report.md");

        when(assetFileFacade.createFromAi(
            eq(3L), eq(2), startsWith("data-analyst-report-"), eq("text/markdown"), eq(report), any(), any(),
            any(), any())).thenReturn(assetFile);

        stubSubagent(report);

        JsonNode node = jsonMapper.readTree(newCallback().call("{\"question\":\"long analysis\"}", toolContext()));

        assertThat(node.get("truncatedIntoFile")
            .asBoolean()).isTrue();
        assertThat(node.get("reportFileId")
            .asString()).isEqualTo("99");
        assertThat(node.get("reportName")
            .asString()).isEqualTo("data-analyst-report.md");
        assertThat(node.get("summary")
            .asString()).hasSize(DataAnalystToolCallback.SUMMARY_PREVIEW_LENGTH + 3);

        verify(artifactRecorder).record("thread-1", 7L, "FILE_CREATED", "99", "data-analyst-report.md");
    }

    @Test
    void testCallReturnsALongReportInlineWithoutAWorkspace() {
        String report = "y".repeat(DataAnalystToolCallback.INLINE_REPORT_MAX_LENGTH + 1);

        stubSubagent(report);

        JsonNode node = jsonMapper.readTree(newCallback().call("{\"question\":\"long analysis\"}"));

        assertThat(node.get("report")
            .asString()).isEqualTo(report);

        verifyNoInteractions(assetFileFacade);
    }

    @Test
    void testCallReturnsErrorWhenQuestionIsBlank() {
        String result = newCallback().call("{\"question\":\" \"}");

        assertThat(result).contains("question is required");

        verifyNoInteractions(chatClient);
    }

    @Test
    void testCallReturnsToolErrorWhenSubagentReturnsNull() {
        stubSubagent(null);

        JsonNode node = jsonMapper.readTree(newCallback().call("{\"question\":\"anything\"}"));

        assertThat(node.get("error")
            .asString()).contains("data_analyst subagent returned null");
    }

    @Test
    void testCallReturnsToolErrorWithoutLeakingTheExceptionMessage() {
        stubSubagent("unused");

        when(responseSpec.content()).thenThrow(new RuntimeException("upstream model 503"));

        JsonNode node = jsonMapper.readTree(newCallback().call("{\"question\":\"anything\"}"));

        assertThat(node.get("error")
            .asString())
                .contains("data_analyst failed")
                .doesNotContain("upstream model 503");
    }

    @Test
    void testCallPropagatesAnUnavailableChatModel() {
        SubAgentChatModelResolver chatModelResolver = mock(SubAgentChatModelResolver.class);

        when(chatModelResolver.resolve(eq(new SubAgentModelRequest(null, null, null))))
            .thenThrow(new ChatModelUnavailableException("no provider"));

        DataAnalystToolCallback callback = new DataAnalystToolCallback(
            candidateChatModel -> chatClient, chatModelResolver, dataTableToolCallbacks, assetFileFacade,
            artifactRecorder);

        assertThatThrownBy(() -> callback.call("{\"question\":\"anything\"}"))
            .isInstanceOf(ChatModelUnavailableException.class);

        verifyNoInteractions(chatClient);
    }

    @Test
    void testCallReturnsErrorOnInvalidJson() {
        assertThat(newCallback().call("not-json")).containsIgnoringCase("invalid input");
    }

    @Test
    void testToolDefinitionIsNamedDataAnalyst() {
        assertThat(newCallback().getToolDefinition()
            .name()).isEqualTo("data_analyst");
    }

    private DataAnalystToolCallback newCallback() {
        return new DataAnalystToolCallback(
            candidateChatModel -> chatClient, null, dataTableToolCallbacks, assetFileFacade, artifactRecorder);
    }

    private void stubSubagent(String content) {
        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(content);
    }

    private static ToolContext toolContext() {
        AiHubToolInvocationContext invocationContext = new AiHubToolInvocationContext(
            3L, 7L, null, null, 2L, "thread-1");

        return new ToolContext(invocationContext.toToolContext());
    }
}
