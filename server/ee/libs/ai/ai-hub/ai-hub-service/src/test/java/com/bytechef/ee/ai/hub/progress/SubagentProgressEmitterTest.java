/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.event.CustomEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Pins the contract of {@link SubagentProgressEmitter} — the static utility that bridges from any
 * {@link SubagentProgressChannel#publish}er to the {@link AgentSubscriber} bound to the current thread by the parent
 * agent. The previous coverage was limited to {@link SubagentProgressChannelTest} (the thread-local queue) — this test
 * pins the emitter's translation of channel publishes into AG-UI {@link CustomEvent}s, the once-per-thread missing-
 * subscriber log guard, and the deliverIsolated isolation of subscriber exceptions.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class SubagentProgressEmitterTest {

    @Test
    void testEmitProgressIsNoOpWhenNoSubscriberBound() {
        // Sanity check — calling emitProgress without runWithSubscriber must not throw. This is the production
        // safety net behind the once-per-thread WARN: a stray caller drops events silently rather than crashing the
        // surrounding tool callback.
        SubagentProgressEmitter.emitProgress("research", "ignored");
    }

    @Test
    void testDrainAndEmitIsNoOpWhenNoSubscriberBound() {
        SubagentProgressEmitter.drainAndEmit();
    }

    @Test
    void testEmitProgressDeliversCustomEventOnBoundSubscriber() {
        AgentSubscriber subscriber = mock(AgentSubscriber.class);

        SubagentProgressEmitter.runWithSubscriber(subscriber, () -> {
            SubagentProgressEmitter.emitProgress("research", "Searching the web");
        });

        ArgumentCaptor<CustomEvent> eventCaptor = ArgumentCaptor.forClass(CustomEvent.class);

        verify(subscriber).onEvent(eventCaptor.capture());
        verify(subscriber).onCustomEvent(any(CustomEvent.class));

        CustomEvent event = eventCaptor.getValue();

        assertThat(event.getName()).isEqualTo(SubagentProgressEmitter.EVENT_NAME);
        assertThat(event.getValue())
            .extracting("subagentName", "text")
            .containsExactly("research", "Searching the web");
    }

    @Test
    void testRunWithSubscriberRestoresPreviousBindingOnExit() {
        AgentSubscriber outer = mock(AgentSubscriber.class);
        AgentSubscriber inner = mock(AgentSubscriber.class);

        SubagentProgressEmitter.runWithSubscriber(outer, () -> {
            SubagentProgressEmitter.runWithSubscriber(inner, () -> {
                SubagentProgressEmitter.emitProgress("data-analyst", "Inner");
            });

            // After the inner runWithSubscriber returns, emitProgress should target the outer subscriber again.
            // Without restoration the outer subscriber would receive zero events here.
            SubagentProgressEmitter.emitProgress("data-analyst", "Outer");
        });

        verify(outer, times(1)).onEvent(any(CustomEvent.class));
        verify(inner, times(1)).onEvent(any(CustomEvent.class));
    }

    @Test
    void testRunWithSubscriberRemovesBindingWhenNoPrevious() {
        AgentSubscriber subscriber = mock(AgentSubscriber.class);

        SubagentProgressEmitter.runWithSubscriber(subscriber, () -> {});

        // After exit with no prior binding, a subsequent emitProgress must drop silently — proving HOLDER.remove()
        // ran. Without this assertion a leaked thread-local would let the next runAgent on the same thread route
        // events into the wrong subscriber.
        SubagentProgressEmitter.emitProgress("data-analyst", "after-exit");

        verify(subscriber, never()).onEvent(any(CustomEvent.class));
    }

    @Test
    void testDrainAndEmitFlushesBufferedChannelEventsToSubscriber() {
        AgentSubscriber subscriber = mock(AgentSubscriber.class);

        SubagentProgressEmitter.runWithSubscriber(subscriber, () -> {
            SubagentProgressChannel.runWithChannel(() -> {
                SubagentProgressChannel.publish("data-analyst", "first");
                SubagentProgressChannel.publish("data-analyst", "second");
                SubagentProgressEmitter.drainAndEmit();
            });
        });

        verify(subscriber, times(2)).onEvent(any(CustomEvent.class));
    }

    @Test
    void testSubscriberOnEventExceptionDoesNotAbortDeliveryToOnCustomEvent() {
        // The deliverIsolated contract: a misbehaving onEvent must not prevent onCustomEvent from being invoked.
        // Without isolation a closed SSE channel that throws from one of the two callbacks would silently drop the
        // other.
        AgentSubscriber subscriber = mock(AgentSubscriber.class);

        doThrow(new RuntimeException("downstream SSE closed"))
            .when(subscriber)
            .onEvent(any(CustomEvent.class));

        SubagentProgressEmitter.runWithSubscriber(subscriber, () -> {
            SubagentProgressEmitter.emitProgress("research", "still-flowing");
        });

        verify(subscriber).onCustomEvent(any(CustomEvent.class));
    }

    @Test
    void testSubscriberOnCustomEventExceptionDoesNotAbortTheCallingTool() {
        AgentSubscriber subscriber = mock(AgentSubscriber.class);

        doThrow(new RuntimeException("downstream SSE closed"))
            .when(subscriber)
            .onCustomEvent(any(CustomEvent.class));

        SubagentProgressEmitter.runWithSubscriber(subscriber, () -> {
            SubagentProgressEmitter.emitProgress("research", "still-flowing");
        });

        // No exception escaping runWithSubscriber proves the calling tool callback survives a misbehaving
        // subscriber. Progress is observability — it must never cause the surrounding tool to fail.
        verify(subscriber).onEvent(any(CustomEvent.class));
    }
}
