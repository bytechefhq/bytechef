/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class DataAnalystToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testCallReturnsInlineReportWhenUnder2000Chars() throws Exception {
        String shortReport = "Analysis complete. Total revenue is $10,000.";

        assertThat(shortReport.length()).isLessThanOrEqualTo(DataAnalystToolCallback.INLINE_REPORT_MAX_LENGTH);

        AssetFileFacade assetFileFacade = mock(AssetFileFacade.class);
        ChatClient dataAnalystChatClient = mock(ChatClient.class);

        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(dataAnalystChatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(shortReport);

        DataAnalystToolCallback callback =
            new DataAnalystToolCallback(dataAnalystChatClient, assetFileFacade);

        String result = callback.call("{\"question\":\"What is the total revenue?\"}");

        assertThat(result).isEqualTo(shortReport);
        verify(assetFileFacade, never()).createFromAi(any(), anyInt(), anyString(), anyString(), anyString(), any(),
            any(), any(), any());
    }

    @Test
    void testCallSpillsLargeReportToAssetFile() throws Exception {
        String largeReport = "A".repeat(3000);

        assertThat(largeReport.length()).isGreaterThan(DataAnalystToolCallback.INLINE_REPORT_MAX_LENGTH);

        long expectedFileId = 42L;

        AssetFile assetFile = new AssetFile();

        assetFile.setId(expectedFileId);

        AssetFileFacade assetFileFacade = mock(AssetFileFacade.class);
        ChatClient dataAnalystChatClient = mock(ChatClient.class);

        when(assetFileFacade.createFromAi(
            eq(1L), anyInt(), anyString(), eq("text/markdown"), eq(largeReport), isNull(), isNull(), isNull(),
            anyString()))
                .thenReturn(assetFile);

        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(dataAnalystChatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(largeReport);

        DataAnalystToolCallback callback =
            new DataAnalystToolCallback(dataAnalystChatClient, assetFileFacade);

        // The pre-existing stub at line 87-89 expects isNull() for sourceOrdinal AND for the format/metadataJson
        // params. The AiHubToolInvocationContext we pass therefore must carry a null sourceOrdinal so the mock
        // matcher fires; passing a literal (short) 0 here would skip the stub, return a null AssetFile, and the
        // production NPE would surface as a tool-error envelope without the reportFileId we assert on.
        ToolContext toolContext = new ToolContext(
            new AiHubToolInvocationContext(1L, 10L, null, "x", 0L, "thread-1").toToolContext());

        String result = callback.call("{\"question\":\"What is the revenue breakdown?\"}", toolContext);

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("reportFileId")).isTrue();
        assertThat(node.get("reportFileId")
            .asLong()).isEqualTo(expectedFileId);
        assertThat(node.has("reportName")).isTrue();
        assertThat(node.get("reportName")
            .asText()).startsWith("data-analyst-report-");
        assertThat(node.has("summary")).isTrue();
        assertThat(node.get("summary")
            .asText()).endsWith("...");
        assertThat(node.get("summary")
            .asText()
            .length()).isLessThanOrEqualTo(DataAnalystToolCallback.SUMMARY_PREVIEW_LENGTH + 3);
        assertThat(node.has("truncatedIntoFile")).isTrue();
        assertThat(node.get("truncatedIntoFile")
            .asBoolean()).isTrue();

        verify(assetFileFacade).createFromAi(
            eq(1L), anyInt(), anyString(), eq("text/markdown"), eq(largeReport), isNull(), isNull(), isNull(),
            anyString());
    }

    @Test
    void testCallReturnsToolErrorWhenSubagentThrows() throws Exception {
        // Regression guard: a Spring AI 4xx/5xx, IO, or NPE from the inner ChatClient call must surface as a
        // typed tool error rather than an uncaught RuntimeException.
        AssetFileFacade assetFileFacade = mock(AssetFileFacade.class);
        ChatClient dataAnalystChatClient = mock(ChatClient.class);

        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(dataAnalystChatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenThrow(new RuntimeException("upstream model 503"));

        DataAnalystToolCallback callback =
            new DataAnalystToolCallback(dataAnalystChatClient, assetFileFacade);

        String result = callback.call("{\"question\":\"Any question\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText())
                .as("payload must surface tool name and exception class for the LLM to recover")
                .contains("data_analyst failed")
                .contains("RuntimeException")
                .as("payload must NOT leak the exception's getMessage() text — see ToolErrors.runtimeFailure")
                .doesNotContain("upstream model 503");

        verify(assetFileFacade, never()).createFromAi(any(), anyInt(), anyString(), anyString(), anyString(), any(),
            any(), any(), any());
    }

    /**
     * Catch-narrowing regression guard. The {@code catch (RuntimeException)} arm in the production code MUST cover
     * every realistic upstream failure mode — Spring AI 4xx/5xx (WebClientResponseException), connection drops
     * (IOException wrapped as RuntimeException), thread interrupts (TimeoutException wrapped), and bare NPEs from
     * malformed responses. A future refactor that narrows to {@code catch (WebClientResponseException)} would let the
     * other four leak again. Using {@code MethodSource} makes the matrix explicit so adding a new exception type to
     * defend against is one-line.
     */
    private static Stream<Arguments> upstreamFailures() {
        return Stream.of(
            Arguments.of(new RuntimeException(WebClientResponseException.create(
                400, "Bad Request", null, null, null)
                .toString())),
            Arguments.of(new RuntimeException(WebClientResponseException.create(
                503, "Service Unavailable", null, null, null)
                .toString())),
            Arguments.of(new RuntimeException(new IOException("connection reset"))),
            Arguments.of(new RuntimeException(new TimeoutException("upstream timeout"))),
            Arguments.of(new NullPointerException("malformed response")));
    }

    @ParameterizedTest
    @MethodSource("upstreamFailures")
    void testCallSurfacesAllRuntimeExceptionTypesAsToolError(RuntimeException upstreamException) throws Exception {
        AssetFileFacade assetFileFacade = mock(AssetFileFacade.class);
        ChatClient dataAnalystChatClient = mock(ChatClient.class);

        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);

        when(dataAnalystChatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenThrow(upstreamException);

        DataAnalystToolCallback callback =
            new DataAnalystToolCallback(dataAnalystChatClient, assetFileFacade);

        String result = callback.call("{\"question\":\"Any question\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error"))
            .as("every upstream RuntimeException must produce a typed tool-error payload, not propagate")
            .isTrue();
        assertThat(node.get("error")
            .asText()).contains("data_analyst failed");
    }
}
