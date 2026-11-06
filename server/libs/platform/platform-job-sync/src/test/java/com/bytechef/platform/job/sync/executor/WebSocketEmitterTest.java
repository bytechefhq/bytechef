/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.job.sync.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WebSocketEmitterTest {

    @Test
    void testGetTimeoutIsNullForTheDefaultConstructor() {
        WebSocketEmitter webSocketEmitter = new WebSocketEmitter();

        assertThat(webSocketEmitter.getTimeout()).isNull();
    }

    @Test
    void testGetTimeoutReturnsTheConfiguredTimeout() {
        WebSocketEmitter webSocketEmitter = new WebSocketEmitter(5000L);

        assertThat(webSocketEmitter.getTimeout()).isEqualTo(5000L);
    }

    @Test
    void testCompleteRunsCloseListenersBeforeCompletionListeners() {
        WebSocketEmitter webSocketEmitter = new WebSocketEmitter();

        List<String> calls = new ArrayList<>();

        webSocketEmitter.addCompletionListener(() -> calls.add("completion"));
        webSocketEmitter.addCloseListener(() -> calls.add("close"));

        webSocketEmitter.complete();

        assertThat(calls).containsExactly("close", "completion");
    }

    @Test
    void testCompleteNotifiesListenersOnlyOnce() {
        WebSocketEmitter webSocketEmitter = new WebSocketEmitter();

        AtomicInteger completionCount = new AtomicInteger();

        webSocketEmitter.addCompletionListener(completionCount::incrementAndGet);

        webSocketEmitter.complete();
        webSocketEmitter.complete();

        assertThat(completionCount.get()).isEqualTo(1);
    }

    @Test
    void testCompleteContinuesPastAFailingListener() {
        WebSocketEmitter webSocketEmitter = new WebSocketEmitter();

        List<String> calls = new ArrayList<>();

        webSocketEmitter.addCloseListener(() -> {
            throw new IllegalStateException("close failed");
        });
        webSocketEmitter.addCloseListener(() -> calls.add("close"));
        webSocketEmitter.addCompletionListener(() -> {
            throw new IllegalStateException("completion failed");
        });
        webSocketEmitter.addCompletionListener(() -> calls.add("completion"));

        assertThatCode(webSocketEmitter::complete).doesNotThrowAnyException();

        assertThat(calls).containsExactly("close", "completion");
    }

    @Test
    void testListenersAddedAfterCompletionRunImmediately() {
        WebSocketEmitter webSocketEmitter = new WebSocketEmitter();

        webSocketEmitter.complete();

        List<String> calls = new ArrayList<>();

        webSocketEmitter.addCloseListener(() -> calls.add("close"));
        webSocketEmitter.addCompletionListener(() -> calls.add("completion"));

        assertThat(calls).containsExactly("close", "completion");
    }

    @Test
    void testListenersAddedAfterCompletionDoNotPropagateTheirFailure() {
        WebSocketEmitter webSocketEmitter = new WebSocketEmitter();

        webSocketEmitter.complete();

        assertThatCode(() -> webSocketEmitter.addCloseListener(() -> {
            throw new IllegalStateException("close failed");
        })).doesNotThrowAnyException();

        assertThatCode(() -> webSocketEmitter.addCompletionListener(() -> {
            throw new IllegalStateException("completion failed");
        })).doesNotThrowAnyException();
    }

    @Test
    void testErrorNotifiesErrorListenersAndThenCompletes() {
        WebSocketEmitter webSocketEmitter = new WebSocketEmitter();

        RuntimeException streamException = new RuntimeException("stream failed");
        List<Object> calls = new ArrayList<>();

        webSocketEmitter.addErrorListener(calls::add);
        webSocketEmitter.addCompletionListener(() -> calls.add("completion"));

        webSocketEmitter.error(streamException);

        assertThat(calls).containsExactly(streamException, "completion");
    }

    @Test
    void testErrorAfterCompletionIsIgnored() {
        WebSocketEmitter webSocketEmitter = new WebSocketEmitter();

        List<Throwable> errors = new ArrayList<>();

        webSocketEmitter.addErrorListener(errors::add);

        webSocketEmitter.complete();
        webSocketEmitter.error(new RuntimeException("late failure"));

        assertThat(errors).isEmpty();
    }

    @Test
    void testErrorListenerAddedAfterAnErrorReceivesTheLastError() {
        WebSocketEmitter webSocketEmitter = new WebSocketEmitter();

        RuntimeException streamException = new RuntimeException("stream failed");

        webSocketEmitter.error(streamException);

        List<Throwable> errors = new ArrayList<>();

        webSocketEmitter.addErrorListener(errors::add);

        assertThat(errors).containsExactly(streamException);
    }

    @Test
    void testErrorListenerAddedAfterACompletionWithoutAnErrorIsNotCalled() {
        WebSocketEmitter webSocketEmitter = new WebSocketEmitter();

        webSocketEmitter.complete();

        List<Throwable> errors = new ArrayList<>();

        webSocketEmitter.addErrorListener(errors::add);

        assertThat(errors).isEmpty();
    }

    @Test
    void testErrorContinuesPastAFailingErrorListener() {
        WebSocketEmitter webSocketEmitter = new WebSocketEmitter();

        RuntimeException streamException = new RuntimeException("stream failed");
        List<Throwable> errors = new ArrayList<>();

        webSocketEmitter.addErrorListener(throwable -> {
            throw new IllegalStateException("listener failed");
        });
        webSocketEmitter.addErrorListener(errors::add);

        assertThatCode(() -> webSocketEmitter.error(streamException)).doesNotThrowAnyException();

        assertThat(errors).containsExactly(streamException);
    }

    @Test
    void testSendNotifiesEveryOutboundListenerEvenWhenOneFails() {
        WebSocketEmitter webSocketEmitter = new WebSocketEmitter();

        List<Object> received = new ArrayList<>();

        webSocketEmitter.addOutboundListener(payload -> {
            throw new IllegalStateException("listener failed");
        });
        webSocketEmitter.addOutboundListener(received::add);

        assertThatCode(() -> webSocketEmitter.send("chunk")).doesNotThrowAnyException();

        assertThat(received).containsExactly("chunk");
    }

    @Test
    void testSendBinaryNotifiesEveryOutboundBinaryListenerEvenWhenOneFails() {
        WebSocketEmitter webSocketEmitter = new WebSocketEmitter();

        byte[] data = {
            1, 2, 3
        };
        List<byte[]> received = new ArrayList<>();
        List<Object> textReceived = new ArrayList<>();

        webSocketEmitter.addOutboundBinaryListener(bytes -> {
            throw new IllegalStateException("listener failed");
        });
        webSocketEmitter.addOutboundBinaryListener(received::add);
        webSocketEmitter.addOutboundListener(textReceived::add);

        assertThatCode(() -> webSocketEmitter.sendBinary(data)).doesNotThrowAnyException();

        assertThat(received).hasSize(1);
        assertThat(received.getFirst()).isSameAs(data);
        assertThat(textReceived).isEmpty();
    }

    @Test
    void testDispatchMessageNotifiesEveryMessageListenerEvenWhenOneFails() {
        WebSocketEmitter webSocketEmitter = new WebSocketEmitter();

        List<Object> received = new ArrayList<>();
        List<Object> outbound = new ArrayList<>();

        webSocketEmitter.addMessageListener(message -> {
            throw new IllegalStateException("listener failed");
        });
        webSocketEmitter.addMessageListener(received::add);
        webSocketEmitter.addOutboundListener(outbound::add);

        assertThatCode(() -> webSocketEmitter.dispatchMessage("hello")).doesNotThrowAnyException();

        assertThat(received).containsExactly("hello");
        assertThat(outbound).isEmpty();
    }

    @Test
    void testDispatchBinaryMessageNotifiesEveryBinaryMessageListenerEvenWhenOneFails() {
        WebSocketEmitter webSocketEmitter = new WebSocketEmitter();

        byte[] data = {
            4, 5
        };
        List<byte[]> received = new ArrayList<>();

        webSocketEmitter.addBinaryMessageListener(bytes -> {
            throw new IllegalStateException("listener failed");
        });
        webSocketEmitter.addBinaryMessageListener(received::add);

        assertThatCode(() -> webSocketEmitter.dispatchBinaryMessage(data)).doesNotThrowAnyException();

        assertThat(received).hasSize(1);
        assertThat(received.getFirst()).isSameAs(data);
    }

    @Test
    void testTriggerTimeoutNotifiesEveryTimeoutListenerEvenWhenOneFails() {
        WebSocketEmitter webSocketEmitter = new WebSocketEmitter();

        AtomicInteger timeoutCount = new AtomicInteger();

        webSocketEmitter.addTimeoutListener(() -> {
            throw new IllegalStateException("listener failed");
        });
        webSocketEmitter.addTimeoutListener(timeoutCount::incrementAndGet);

        assertThatCode(webSocketEmitter::triggerTimeout).doesNotThrowAnyException();

        assertThat(timeoutCount.get()).isEqualTo(1);
    }

    @Test
    void testTimeoutListenerAddedAfterCompletionIsNeverCalled() {
        WebSocketEmitter webSocketEmitter = new WebSocketEmitter();

        AtomicInteger timeoutCount = new AtomicInteger();

        webSocketEmitter.complete();

        webSocketEmitter.addTimeoutListener(timeoutCount::incrementAndGet);

        webSocketEmitter.triggerTimeout();

        assertThat(timeoutCount.get()).isZero();
    }
}
