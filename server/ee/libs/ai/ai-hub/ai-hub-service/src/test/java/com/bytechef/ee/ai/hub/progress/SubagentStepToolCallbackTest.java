/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.progress;

import static org.assertj.core.api.Assertions.assertThat;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.event.CustomEvent;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SubagentStepToolCallbackTest {

    private final List<String> emitted = new CopyOnWriteArrayList<>();
    private final List<String> toolCalls = new CopyOnWriteArrayList<>();

    @Test
    void testWithoutAScopeTheToolRunsUnchanged() {
        ToolCallback toolCallback = SubagentStepToolCallback.wrap(searchComponents());

        String result = toolCallback.call("{\"query\":\"slack\"}", new ToolContext(Map.of("tenantId", "acme")));

        assertThat(result).isEqualTo("\"found\"");
        assertThat(toolCalls).containsExactly("searchComponents");
        assertThat(emitted).isEmpty();
    }

    @Test
    void testInsideAScopeTheToolReportsItsStepBeforeRunning() {
        SubagentProgressEmitter emitter = new SubagentProgressEmitter(new AgentSubscriber() {

            @Override
            @SuppressWarnings("unchecked")
            public void onCustomEvent(CustomEvent event) {
                Map<String, Object> value = (Map<String, Object>) event.getValue();

                emitted.add(value.get("phase") + ": " + value.get("text"));
                toolCalls.add("event");
            }
        });

        SubagentProgressScope scope = SubagentProgressScope.open(emitter, null, "buildWorkflow", "Build it");

        ToolCallback toolCallback = SubagentStepToolCallback.wrap(searchComponents());

        String result = toolCallback.call("{\"query\":\"slack\"}", scope.attachTo(null));

        assertThat(result).isEqualTo("\"found\"");
        assertThat(emitted).containsExactly("started: Build it", "step: Searching components");
        assertThat(toolCalls).containsExactly("event", "event", "searchComponents");
    }

    @Test
    void testWrappingTwiceKeepsOneWrapper() {
        ToolCallback toolCallback = SubagentStepToolCallback.wrap(searchComponents());

        assertThat(SubagentStepToolCallback.wrap(toolCallback)).isSameAs(toolCallback);
    }

    private ToolCallback searchComponents() {
        return FunctionToolCallback.builder("searchComponents", (Map<String, Object> input) -> {
            toolCalls.add("searchComponents");

            return "found";
        })
            .description("Search the components")
            .inputType(Map.class)
            .build();
    }
}
