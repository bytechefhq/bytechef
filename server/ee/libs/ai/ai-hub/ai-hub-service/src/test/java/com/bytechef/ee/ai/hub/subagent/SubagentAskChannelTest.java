/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.subagent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SubagentAskChannelTest {

    private static final String ASK_PAYLOAD = "{\"kind\":\"ask-user-question\"}";

    @Test
    void testOfferedPayloadIsVisibleWithinTheChannel() {
        String result = SubagentAskChannel.runWithChannel(() -> {
            SubagentAskChannel.offer(ASK_PAYLOAD);

            return SubagentAskChannel.pending();
        });

        assertThat(result).isEqualTo(ASK_PAYLOAD);
    }

    @Test
    void testSecondOfferInOneDelegationIsRejected() {
        Boolean secondOfferAccepted = SubagentAskChannel.runWithChannel(() -> {
            SubagentAskChannel.offer(ASK_PAYLOAD);

            return SubagentAskChannel.offer(ASK_PAYLOAD);
        });

        assertThat(secondOfferAccepted).isFalse();
    }

    @Test
    void testFirstOfferSurvivesARejectedSecondOffer() {
        String result = SubagentAskChannel.runWithChannel(() -> {
            SubagentAskChannel.offer(ASK_PAYLOAD);
            SubagentAskChannel.offer("{\"kind\":\"ask-user-question\",\"questions\":[\"second\"]}");

            return SubagentAskChannel.pending();
        });

        assertThat(result).isEqualTo(ASK_PAYLOAD);
    }

    @Test
    void testOfferOutsideAChannelIsIgnored() {
        assertThat(SubagentAskChannel.offer(ASK_PAYLOAD)).isFalse();
        assertThat(SubagentAskChannel.pending()).isNull();
    }

    @Test
    void testNestedChannelsRestoreTheOuterBinding() {
        String outerPending = SubagentAskChannel.runWithChannel(() -> {
            SubagentAskChannel.offer("outer");

            SubagentAskChannel.runWithChannel(() -> {
                SubagentAskChannel.offer("inner");

                return null;
            });

            return SubagentAskChannel.pending();
        });

        assertThat(outerPending).isEqualTo("outer");
    }

    /**
     * Tool-execution threads are pooled, so a binding left behind would make an unrelated later delegation surface a
     * stale question card.
     */
    @Test
    void testChannelIsUnboundAfterTheSupplierReturns() {
        SubagentAskChannel.runWithChannel(() -> SubagentAskChannel.offer(ASK_PAYLOAD));

        assertThat(SubagentAskChannel.pending()).isNull();
    }

    @Test
    void testChannelIsUnboundAfterTheSupplierThrows() {
        try {
            SubagentAskChannel.runWithChannel(() -> {
                SubagentAskChannel.offer(ASK_PAYLOAD);

                throw new IllegalStateException("specialist blew up");
            });
        } catch (IllegalStateException exception) {
            // Expected — the point of the test is what the channel does afterwards.
            assertThat(exception).hasMessage("specialist blew up");
        }

        assertThat(SubagentAskChannel.pending()).isNull();
    }
}
