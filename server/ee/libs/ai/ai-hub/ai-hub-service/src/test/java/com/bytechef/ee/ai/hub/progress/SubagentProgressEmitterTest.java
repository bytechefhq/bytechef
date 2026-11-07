/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.event.BaseEvent;
import com.agui.core.event.CustomEvent;
import com.bytechef.ee.ai.hub.progress.SubagentProgressEmitter.Phase;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SubagentProgressEmitterTest {

    @Test
    void testEmitsANamedCustomEventToBothSubscriberCallbacks() {
        List<BaseEvent> events = new CopyOnWriteArrayList<>();
        List<CustomEvent> customEvents = new CopyOnWriteArrayList<>();

        SubagentProgressEmitter emitter = new SubagentProgressEmitter(new AgentSubscriber() {

            @Override
            public void onEvent(BaseEvent event) {
                events.add(event);
            }

            @Override
            public void onCustomEvent(CustomEvent event) {
                customEvents.add(event);
            }
        });

        emitter.emit("call-1", "buildWorkflow", Phase.STEP, "Searching components");

        assertThat(events).singleElement()
            .isSameAs(customEvents.getFirst());
        assertThat(customEvents.getFirst()
            .getName()).isEqualTo(SubagentProgressEmitter.EVENT_NAME);
        assertThat(customEvents.getFirst()
            .getValue()).isEqualTo(
                Map.of(
                    "toolCallId", "call-1", "subagentName", "buildWorkflow", "phase", "step", "text",
                    "Searching components"));
    }

    @Test
    void testASubscriberFailureNeverReachesTheTool() {
        SubagentProgressEmitter emitter = new SubagentProgressEmitter(new AgentSubscriber() {

            @Override
            public void onEvent(BaseEvent event) {
                throw new IllegalStateException("stream closed");
            }
        });

        assertThatCode(() -> emitter.emit(null, "buildWorkflow", Phase.DONE, "done")).doesNotThrowAnyException();
    }

    @Test
    void testFetchFindsOnlyAnEmitterBoundUnderItsKey() {
        SubagentProgressEmitter emitter = new SubagentProgressEmitter(new AgentSubscriber() {});

        assertThat(SubagentProgressEmitter.fetch(new ToolContext(Map.of(SubagentProgressEmitter.TOOL_CONTEXT_KEY,
            emitter)))).isSameAs(emitter);
        assertThat(SubagentProgressEmitter.fetch(new ToolContext(Map.of(SubagentProgressEmitter.TOOL_CONTEXT_KEY,
            "not an emitter")))).isNull();
        assertThat(SubagentProgressEmitter.fetch(new ToolContext(Map.of()))).isNull();
        assertThat(SubagentProgressEmitter.fetch(null)).isNull();
    }

    @Test
    void testPhasesUseLowerCaseWireValues() {
        assertThat(Phase.values()).extracting(Phase::wireValue)
            .containsExactly("started", "step", "done", "failed");
    }
}
