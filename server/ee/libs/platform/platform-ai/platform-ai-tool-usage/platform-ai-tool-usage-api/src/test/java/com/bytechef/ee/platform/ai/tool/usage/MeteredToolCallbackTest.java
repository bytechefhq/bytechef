/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.tool.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class MeteredToolCallbackTest {

    private static final ToolUsageContext TOOL_USAGE_CONTEXT = new ToolUsageContext(1L, 2L, 0, 3L);

    private final ToolCallback delegate = mock(ToolCallback.class);
    private final JsonMapper jsonMapper = JsonMapper.builder()
        .build();
    private final ToolUsageRecorder toolUsageRecorder = mock(ToolUsageRecorder.class);

    @Test
    void testCallRecordsSuccessfulResponseWithMetadata() {
        ToolContext toolContext = new ToolContext(Map.of());

        when(delegate.call("{\"size\":\"1024x1024\"}", toolContext)).thenReturn("{\"url\":\"https://example.com\"}");

        MeteredToolCallback meteredToolCallback = createMeteredToolCallback(context -> TOOL_USAGE_CONTEXT);

        String response = meteredToolCallback.call("{\"size\":\"1024x1024\"}", toolContext);

        assertThat(response).isEqualTo("{\"url\":\"https://example.com\"}");

        verify(toolUsageRecorder).recordTool(
            eq(TOOL_USAGE_CONTEXT), eq("openai_image"), eq(1), anyLong(), eq(Map.of("size", "1024x1024")));
    }

    @Test
    void testCallSkipsRecordingForErrorResponse() {
        when(delegate.call(anyString(), any())).thenReturn("{\"error\":\"rate limited\"}");

        MeteredToolCallback meteredToolCallback = createMeteredToolCallback(context -> TOOL_USAGE_CONTEXT);

        String response = meteredToolCallback.call("{}", new ToolContext(Map.of()));

        assertThat(response).isEqualTo("{\"error\":\"rate limited\"}");

        verify(toolUsageRecorder, never()).recordTool(any(), anyString(), anyInt(), anyLong(), any());
    }

    @Test
    void testCallSkipsRecordingForUnparseableResponse() {
        when(delegate.call(anyString(), any())).thenReturn("not json");

        MeteredToolCallback meteredToolCallback = createMeteredToolCallback(context -> TOOL_USAGE_CONTEXT);

        meteredToolCallback.call("{}", new ToolContext(Map.of()));

        verify(toolUsageRecorder, never()).recordTool(any(), anyString(), anyInt(), anyLong(), any());
    }

    @Test
    void testCallSkipsRecordingWhenContextUnavailable() {
        when(delegate.call(anyString(), any())).thenReturn("{\"ok\":true}");

        MeteredToolCallback meteredToolCallback = createMeteredToolCallback(context -> null);

        String response = meteredToolCallback.call("{}", null);

        assertThat(response).isEqualTo("{\"ok\":true}");

        verify(toolUsageRecorder, never()).recordTool(any(), anyString(), anyInt(), anyLong(), any());
    }

    @Test
    void testCallRecordsBlankResponseWithoutMetadata() {
        when(delegate.call(anyString(), any())).thenReturn("");

        MeteredToolCallback meteredToolCallback = createMeteredToolCallback(context -> TOOL_USAGE_CONTEXT);

        meteredToolCallback.call("{\"quality\":\"hd\"}", new ToolContext(Map.of()));

        verify(toolUsageRecorder).recordTool(
            eq(TOOL_USAGE_CONTEXT), eq("openai_image"), eq(1), anyLong(), eq(null));
    }

    private MeteredToolCallback createMeteredToolCallback(
        Function<ToolContext, ToolUsageContext> contextResolver) {

        return new MeteredToolCallback(
            delegate, "openai_image", 1, toolUsageRecorder, contextResolver,
            MeteredToolCallback.singleStringField("size"), jsonMapper);
    }
}
