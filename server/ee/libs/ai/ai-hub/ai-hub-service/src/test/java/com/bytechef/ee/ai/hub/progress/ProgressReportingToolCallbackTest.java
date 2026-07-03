/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.event.BaseEvent;
import com.agui.core.event.CustomEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProgressReportingToolCallbackTest {

    @Test
    void testDelegatesCallToInnerCallback() {
        ToolCallback delegate = mock(ToolCallback.class);

        when(delegate.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name("research")
                .description("Research")
                .inputSchema("{}")
                .build());
        when(delegate.call(anyString(), any())).thenReturn("{\"report\":\"test\"}");

        ProgressReportingToolCallback wrapper = new ProgressReportingToolCallback(delegate, "research");

        String result = wrapper.call("{\"topic\":\"AI trends\"}", null);

        assertThat(result).isEqualTo("{\"report\":\"test\"}");
        verify(delegate).call("{\"topic\":\"AI trends\"}", null);
    }

    @Test
    void testEmitsProgressEventViaSubscriberBeforeCallWhenSubscriberBound() {
        ToolCallback delegate = mock(ToolCallback.class);

        when(delegate.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name("research")
                .description("Research")
                .inputSchema("{}")
                .build());
        when(delegate.call(anyString(), any())).thenReturn("{\"report\":\"ok\"}");

        ProgressReportingToolCallback wrapper = new ProgressReportingToolCallback(delegate, "research");

        List<BaseEvent> emittedEvents = new ArrayList<>();
        AgentSubscriber subscriber = mock(AgentSubscriber.class);

        org.mockito.Mockito.doAnswer(invocation -> {
            emittedEvents.add(invocation.getArgument(0));

            return null;
        })
            .when(subscriber)
            .onEvent(any());

        SubagentProgressEmitter.runWithSubscriber(subscriber, () -> wrapper.call("{\"topic\":\"AI trends\"}", null));

        // Two events fire per call: a "start" with the input preview, then a paired "done"/"failed" so the UI's
        // running indicator decrements regardless of delegate outcome. Without the second emit, a delegate that
        // throws would leave the subagent stuck visually as "still working" (see ProgressReportingToolCallback).
        assertThat(emittedEvents).hasSize(2);
        assertThat(emittedEvents.getFirst()).isInstanceOf(CustomEvent.class);
        assertThat(emittedEvents.getLast()).isInstanceOf(CustomEvent.class);

        CustomEvent startEvent = (CustomEvent) emittedEvents.getFirst();

        assertThat(startEvent.getName()).isEqualTo(SubagentProgressEmitter.EVENT_NAME);
        assertThat(startEvent.getValue()).isNotNull();

        CustomEvent completionEvent = (CustomEvent) emittedEvents.getLast();

        assertThat(completionEvent.getName()).isEqualTo(SubagentProgressEmitter.EVENT_NAME);
        assertThat(completionEvent.getValue())
            .as("delegate succeeded, so the completion event must signal `done` (not `failed`)")
            .asString()
            .contains("done");
    }

    @Test
    void testEmitsFailedCompletionEventWhenDelegateThrows() {
        // Pins the paired-completion contract under the failure path. Without this test, a regression that
        // collapsed try/finally back to a bare delegate.call() would still pass the happy-path assertion above
        // but leave the UI stuck on a delegate that threw.
        ToolCallback delegate = mock(ToolCallback.class);

        when(delegate.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name("research")
                .description("Research")
                .inputSchema("{}")
                .build());
        when(delegate.call(anyString(), any())).thenThrow(new RuntimeException("upstream model 503"));

        ProgressReportingToolCallback wrapper = new ProgressReportingToolCallback(delegate, "research");

        List<BaseEvent> emittedEvents = new ArrayList<>();
        AgentSubscriber subscriber = mock(AgentSubscriber.class);

        org.mockito.Mockito.doAnswer(invocation -> {
            emittedEvents.add(invocation.getArgument(0));

            return null;
        })
            .when(subscriber)
            .onEvent(any());

        org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> SubagentProgressEmitter.runWithSubscriber(
                subscriber, () -> wrapper.call("{\"topic\":\"AI\"}", null)))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("upstream model 503");

        assertThat(emittedEvents).hasSize(2);

        CustomEvent completionEvent = (CustomEvent) emittedEvents.getLast();

        assertThat(completionEvent.getValue())
            .as("delegate threw, so the completion event must signal `failed`")
            .asString()
            .contains("failed");
    }

    @Test
    void testInputPreviewIsTruncatedTo80Chars() {
        ToolCallback delegate = mock(ToolCallback.class);

        when(delegate.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name("research")
                .description("Research")
                .inputSchema("{}")
                .build());
        when(delegate.call(anyString(), any())).thenReturn("{}");

        ProgressReportingToolCallback wrapper = new ProgressReportingToolCallback(delegate, "research");

        List<BaseEvent> emittedEvents = new ArrayList<>();
        AgentSubscriber subscriber = mock(AgentSubscriber.class);

        org.mockito.Mockito.doAnswer(invocation -> {
            emittedEvents.add(invocation.getArgument(0));

            return null;
        })
            .when(subscriber)
            .onEvent(any());

        String longInput = "a".repeat(200);

        SubagentProgressEmitter.runWithSubscriber(subscriber, () -> wrapper.call(longInput, null));

        CustomEvent customEvent = (CustomEvent) emittedEvents.getFirst();
        Object value = customEvent.getValue();

        assertThat(value.toString()).contains("…");
    }

    @Test
    void testNoEventEmittedWhenNoSubscriberBound() {
        // When no subscriber is bound to the SubagentProgressEmitter holder, the wrapper must still delegate
        // the call and return the delegate's result — but the absent subscriber must be a silent no-op, not
        // an exception that aborts the tool call.
        ToolCallback delegate = mock(ToolCallback.class);

        when(delegate.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name("research")
                .description("Research")
                .inputSchema("{}")
                .build());
        when(delegate.call(anyString(), any())).thenReturn("{\"ok\":true}");

        ProgressReportingToolCallback wrapper = new ProgressReportingToolCallback(delegate, "research");

        String result = wrapper.call("{\"topic\":\"AI\"}", null);

        assertThat(result).isEqualTo("{\"ok\":true}");
        verify(delegate).call(eq("{\"topic\":\"AI\"}"), any());
    }
}
