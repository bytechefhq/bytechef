/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.event.BaseEvent;
import com.agui.core.event.CustomEvent;
import com.agui.spring.ai.AgUiFunctionToolCallback;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SubagentProgressToolCallbackTest {

    private static final String INPUT = "{\"request\":\"Build a workflow that sends a daily report\"}";

    private final List<BaseEvent> events = new CopyOnWriteArrayList<>();
    private final SubagentProgressEmitter emitter = new SubagentProgressEmitter(new AgentSubscriber() {

        @Override
        public void onEvent(BaseEvent event) {
            events.add(event);
        }
    });

    @Test
    void testWithoutAnEmitterTheCallPassesThroughUnchanged() {
        AtomicReference<ToolContext> receivedContext = new AtomicReference<>();

        ToolCallback toolCallback = SubagentProgressToolCallback.wrap(
            delegate((toolInput, toolContext) -> {
                receivedContext.set(toolContext);

                return "{\"workflowId\":\"w-1\"}";
            }));

        ToolContext toolContext = new ToolContext(Map.of("tenantId", "acme"));

        assertThat(toolCallback.call(INPUT, toolContext)).isEqualTo("{\"workflowId\":\"w-1\"}");
        assertThat(receivedContext.get()).isSameAs(toolContext);
        assertThat(toolCallback.call(INPUT, null)).isEqualTo("{\"workflowId\":\"w-1\"}");
        assertThat(receivedContext.get()).isNull();
    }

    @Test
    void testEmitsStartedAndDoneAroundTheDelegateAndHandsItTheScope() {
        AtomicReference<SubagentProgressScope> receivedScope = new AtomicReference<>();

        ToolCallback toolCallback = SubagentProgressToolCallback.wrap(
            delegate((toolInput, toolContext) -> {
                receivedScope.set(SubagentProgressScope.fetch(toolContext));

                return "{\"workflowId\":\"w-1\"}";
            }));

        String result = toolCallback.call(INPUT, toolContext("call-1"));

        assertThat(result).isEqualTo("{\"workflowId\":\"w-1\"}");
        assertThat(receivedScope.get()).isNotNull();
        assertThat(progressValues()).containsExactly(
            Map.of(
                "toolCallId", "call-1", "subagentName", "buildWorkflow", "phase", "started",
                "text", "Build a workflow that sends a daily report"),
            Map.of("toolCallId", "call-1", "subagentName", "buildWorkflow", "phase", "done", "text", "done"));
    }

    @Test
    void testEmitsFailedAndRethrowsWhenTheDelegateThrows() {
        IllegalStateException failure = new IllegalStateException("model unavailable");

        ToolCallback toolCallback = SubagentProgressToolCallback.wrap(
            delegate((toolInput, toolContext) -> {
                throw failure;
            }));

        assertThatThrownBy(() -> toolCallback.call(INPUT, toolContext("call-1"))).isSameAs(failure);
        assertThat(progressValues()).extracting(value -> value.get("phase"))
            .containsExactly("started", "failed");
    }

    @Test
    void testEmitsFailedWhenTheDelegateReturnsAToolError() {
        ToolCallback toolCallback = SubagentProgressToolCallback.wrap(
            delegate((toolInput, toolContext) -> "{\"error\":\"buildWorkflow failed (NullPointerException)\"}"));

        String result = toolCallback.call(INPUT, toolContext("call-1"));

        assertThat(result).isEqualTo("{\"error\":\"buildWorkflow failed (NullPointerException)\"}");
        assertThat(progressValues()).extracting(value -> value.get("phase"))
            .containsExactly("started", "failed");
    }

    @Test
    void testANestedDelegateReportsAStepOfTheEnclosingDelegate() {
        ToolCallback innerDelegate = SubagentProgressToolCallback.wrap(
            delegate((toolInput, toolContext) -> "{\"answer\":42}"), "research");

        ToolCallback toolCallback = SubagentProgressToolCallback.wrap(
            delegate((toolInput, toolContext) -> innerDelegate.call(
                "{\"request\":\"Find the Slack rate limits\"}", toolContext)));

        toolCallback.call(INPUT, toolContext("call-1"));

        assertThat(progressValues()).extracting(value -> value.get("phase") + ": " + value.get("text"))
            .containsExactly(
                "started: Build a workflow that sends a daily report",
                "step: research: Find the Slack rate limits",
                "done: done");
    }

    @Test
    void testOmitsTheToolCallIdWhenTheStreamCarriesNone() {
        ToolCallback toolCallback = SubagentProgressToolCallback.wrap(delegate((toolInput, toolContext) -> "{}"));

        toolCallback.call(INPUT, new ToolContext(Map.of(SubagentProgressEmitter.TOOL_CONTEXT_KEY, emitter)));

        assertThat(progressValues()).allSatisfy(value -> assertThat(value).doesNotContainKey("toolCallId"));
    }

    @Test
    void testWrappingTwiceKeepsOneWrapperAndTheDelegateDefinition() {
        ToolCallback delegate = delegate((toolInput, toolContext) -> "{}");

        ToolCallback toolCallback = SubagentProgressToolCallback.wrap(delegate);

        assertThat(SubagentProgressToolCallback.wrap(toolCallback)).isSameAs(toolCallback);
        assertThat(toolCallback.getToolDefinition()).isSameAs(delegate.getToolDefinition());
        assertThat(((SubagentProgressToolCallback) toolCallback).getDelegate()).isSameAs(delegate);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> progressValues() {
        return events.stream()
            .filter(CustomEvent.class::isInstance)
            .map(event -> (Map<String, Object>) ((CustomEvent) event).getValue())
            .toList();
    }

    private ToolContext toolContext(String toolCallId) {
        return new ToolContext(
            Map.of(SubagentProgressEmitter.TOOL_CONTEXT_KEY, emitter, AgUiFunctionToolCallback.TOOL_CALL_ID_KEY,
                toolCallId));
    }

    private static ToolCallback delegate(BiFunction<String, @Nullable ToolContext, String> function) {
        ToolDefinition toolDefinition = ToolDefinition.builder()
            .name("buildWorkflow")
            .description("Build a workflow")
            .inputSchema("{\"type\":\"object\"}")
            .build();

        return new ToolCallback() {

            @Override
            public ToolDefinition getToolDefinition() {
                return toolDefinition;
            }

            @Override
            public String call(String toolInput) {
                return call(toolInput, null);
            }

            @Override
            public String call(String toolInput, @Nullable ToolContext toolContext) {
                return function.apply(toolInput, toolContext);
            }
        };
    }
}
