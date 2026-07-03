/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.progress;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SubagentProgressChannelTest {

    @Test
    void testDrainReturnsEmptyListWhenNoChannelActive() {
        List<SubagentProgressChannel.SubagentProgressEvent> events = SubagentProgressChannel.drain();

        assertThat(events).isEmpty();
    }

    @Test
    void testPublishIsIgnoredWhenNoChannelActive() {
        SubagentProgressChannel.publish("research", "Searching…");

        List<SubagentProgressChannel.SubagentProgressEvent> events = SubagentProgressChannel.drain();

        assertThat(events).isEmpty();
    }

    @Test
    void testRunWithChannelBindsChannelAndPublishWorks() {
        List<SubagentProgressChannel.SubagentProgressEvent> captured = new java.util.ArrayList<>();

        SubagentProgressChannel.runWithChannel(() -> {
            SubagentProgressChannel.publish("research", "Fetching page 1");
            SubagentProgressChannel.publish("research", "Fetching page 2");

            captured.addAll(SubagentProgressChannel.drain());
        });

        assertThat(captured).hasSize(2);
        assertThat(captured.get(0)
            .subagentName()).isEqualTo("research");
        assertThat(captured.get(0)
            .text()).isEqualTo("Fetching page 1");
        assertThat(captured.get(1)
            .text()).isEqualTo("Fetching page 2");
    }

    @Test
    void testDrainClearsChannelAfterCall() {
        SubagentProgressChannel.runWithChannel(() -> {
            SubagentProgressChannel.publish("data_analyst", "Querying table");

            List<SubagentProgressChannel.SubagentProgressEvent> first = SubagentProgressChannel.drain();
            List<SubagentProgressChannel.SubagentProgressEvent> second = SubagentProgressChannel.drain();

            assertThat(first).hasSize(1);
            assertThat(second).isEmpty();
        });
    }

    @Test
    void testIsActiveReturnsFalseOutsideChannel() {
        assertThat(SubagentProgressChannel.isActive()).isFalse();
    }

    @Test
    void testIsActiveReturnsTrueInsideChannel() {
        SubagentProgressChannel.runWithChannel(() -> assertThat(SubagentProgressChannel.isActive()).isTrue());
    }

    @Test
    void testChannelIsRemovedAfterRunWithChannel() {
        SubagentProgressChannel.runWithChannel(() -> {});

        assertThat(SubagentProgressChannel.isActive()).isFalse();
    }
}
