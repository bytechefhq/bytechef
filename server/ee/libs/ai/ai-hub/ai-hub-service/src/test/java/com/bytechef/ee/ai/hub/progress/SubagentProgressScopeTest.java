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
class SubagentProgressScopeTest {

    private final List<String> emitted = new CopyOnWriteArrayList<>();
    private final SubagentProgressEmitter emitter = new SubagentProgressEmitter(new AgentSubscriber() {

        @Override
        @SuppressWarnings("unchecked")
        public void onCustomEvent(CustomEvent event) {
            Map<String, Object> value = (Map<String, Object>) event.getValue();

            emitted.add(value.get("phase") + ": " + value.get("text"));
        }
    });

    @Test
    void testCoalescesIdenticalConsecutiveStepsButKeepsALaterRepeat() {
        SubagentProgressScope scope = SubagentProgressScope.open(emitter, null, "buildWorkflow", "Build it");

        scope.step("Searching components");
        scope.step("Searching components");
        scope.step("Configuring \"slack › sendChannelMessage\"");
        scope.step("Searching components");
        scope.step(" ");

        assertThat(emitted).containsExactly(
            "started: Build it", "step: Searching components", "step: Configuring \"slack › sendChannelMessage\"",
            "step: Searching components");
    }

    @Test
    void testCapsTheNumberOfSteps() {
        SubagentProgressScope scope = SubagentProgressScope.open(emitter, null, "buildWorkflow", "");

        for (int index = 0; index < SubagentProgressScope.MAX_STEPS + 20; index++) {
            scope.step("Step " + index);
        }

        assertThat(emitted).filteredOn(text -> text.startsWith("step: "))
            .hasSize(SubagentProgressScope.MAX_STEPS);
    }

    @Test
    void testIgnoresStepsAndASecondCloseAfterClosing() {
        SubagentProgressScope scope = SubagentProgressScope.open(emitter, null, "buildWorkflow", "");

        scope.close(true);
        scope.step("Saving the workflow");
        scope.close(false);

        assertThat(emitted).containsExactly("started: ", "failed: failed");
    }

    @Test
    void testAttachesItselfToACopyOfTheToolContext() {
        SubagentProgressScope scope = SubagentProgressScope.open(emitter, null, "buildWorkflow", "");

        ToolContext toolContext = new ToolContext(Map.of("tenantId", "acme"));

        ToolContext attached = scope.attachTo(toolContext);

        assertThat(SubagentProgressScope.fetch(attached)).isSameAs(scope);
        assertThat(attached.getContext()).containsEntry("tenantId", "acme");
        assertThat(SubagentProgressScope.fetch(toolContext)).isNull();
        assertThat(SubagentProgressScope.fetch(null)).isNull();
        assertThat(SubagentProgressScope.fetch(scope.attachTo(null))).isSameAs(scope);
    }
}
