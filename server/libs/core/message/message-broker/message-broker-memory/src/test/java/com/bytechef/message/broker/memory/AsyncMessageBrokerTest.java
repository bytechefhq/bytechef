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

package com.bytechef.message.broker.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.message.route.MessageRoute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Ivica Cardic
 */
class AsyncMessageBrokerTest {

    private AsyncMessageBroker broker;

    @BeforeEach
    void setUp() {
        broker = new AsyncMessageBroker(new MockEnvironment());
    }

    @AfterEach
    void tearDown() {
        // Shut down all executors so test threads don't leak between cases and the JVM can exit
        // cleanly after the suite (the cached thread pool's workers are non-daemon).
        broker.shutdown();
    }

    @Test
    void orderedRoutePreservesFifoUnderConcurrentSends() throws InterruptedException {
        List<Integer> received = Collections.synchronizedList(new ArrayList<>());
        int messageCount = 500;
        CountDownLatch latch = new CountDownLatch(messageCount);

        broker.receive(TestRoute.ORDERED, message -> {
            received.add((Integer) message);

            latch.countDown();
        });

        // Producer is single-threaded to establish a send-order ground truth; the broker's internal
        // dispatch is what we're stressing. The earlier cached-thread-pool path would reorder these
        // because each send() spawned a new executor task.
        for (int i = 0; i < messageCount; i++) {
            broker.send(TestRoute.ORDERED, i);
        }

        assertThat(latch.await(10, TimeUnit.SECONDS))
            .as("all messages should be delivered within timeout")
            .isTrue();
        assertThat(received)
            .as("ordered route must preserve send order")
            .containsExactlyElementsOf(IntStream.range(0, messageCount)
                .boxed()
                .collect(Collectors.toList()));
    }

    @Test
    void unorderedRouteStillDispatchesAcrossMultipleThreads() throws InterruptedException {
        Set<String> dispatchThreads = Collections.synchronizedSet(new HashSet<>());
        int messageCount = 50;
        CountDownLatch allRunning = new CountDownLatch(messageCount);
        CountDownLatch releaseAll = new CountDownLatch(1);

        broker.receive(TestRoute.UNORDERED, message -> {
            Thread currentThread = Thread.currentThread();

            dispatchThreads.add(currentThread.getName());

            allRunning.countDown();

            try {
                releaseAll.await();
            } catch (InterruptedException e) {
                currentThread.interrupt();
            }
        });

        for (int i = 0; i < messageCount; i++) {
            broker.send(TestRoute.UNORDERED, i);
        }

        assertThat(allRunning.await(10, TimeUnit.SECONDS))
            .as("unordered dispatch must let all receivers run concurrently")
            .isTrue();

        releaseAll.countDown();

        assertThat(dispatchThreads)
            .as("unordered route should fan out across the cached thread pool")
            .hasSizeGreaterThan(1);
    }

    @Test
    void differentOrderedRoutesRunOnIndependentThreads() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        List<String> routeAThreadNames = Collections.synchronizedList(new ArrayList<>());
        List<String> routeBThreadNames = Collections.synchronizedList(new ArrayList<>());

        broker.receive(TestRoute.ORDERED, message -> {
            routeAThreadNames.add(
                Thread.currentThread()
                    .getName());

            latch.countDown();
        });

        broker.receive(TestRoute.ORDERED_OTHER, message -> {
            routeBThreadNames.add(
                Thread.currentThread()
                    .getName());

            latch.countDown();
        });

        broker.send(TestRoute.ORDERED, 1);
        broker.send(TestRoute.ORDERED_OTHER, 2);

        assertThat(latch.await(5, TimeUnit.SECONDS))
            .as("both receivers should be invoked")
            .isTrue();
        assertThat(routeAThreadNames)
            .as("route A thread name should not overlap with route B")
            .doesNotContainAnyElementsOf(routeBThreadNames);
    }

    @Test
    void orderedRoutePreservesFifoAcrossConcurrentProducers() throws InterruptedException {
        List<Integer> received = Collections.synchronizedList(new ArrayList<>());
        int messagesPerProducer = 200;
        int producerCount = 4;
        int total = messagesPerProducer * producerCount;
        CountDownLatch latch = new CountDownLatch(total);

        broker.receive(TestRoute.ORDERED, message -> {
            received.add((Integer) message);

            latch.countDown();
        });

        ExecutorService producers = Executors.newFixedThreadPool(producerCount);

        try {
            for (int p = 0; p < producerCount; p++) {
                int producerId = p;

                producers.submit(() -> {
                    for (int i = 0; i < messagesPerProducer; i++) {
                        broker.send(TestRoute.ORDERED, producerId * messagesPerProducer + i);
                    }
                });
            }

            assertThat(latch.await(15, TimeUnit.SECONDS))
                .as("all messages should be delivered")
                .isTrue();
        } finally {
            producers.shutdownNow();
        }

        // Per-producer sequences must remain in send-order once interleaved at the receiver. A broker
        // without the per-route serial executor would drop this guarantee because each send() would
        // dispatch on its own pool thread.
        for (int p = 0; p < producerCount; p++) {
            int base = p * messagesPerProducer;
            List<Integer> producerReceived = received.stream()
                .filter(value -> value >= base && value < base + messagesPerProducer)
                .toList();

            assertThat(producerReceived)
                .as("messages from producer %d must be received in send order", p)
                .containsExactlyElementsOf(IntStream.range(base, base + messagesPerProducer)
                    .boxed()
                    .collect(Collectors.toList()));
        }
    }

    @Test
    void orderedRouteBackpressuresProducerWithoutDroppingMessages() throws InterruptedException {
        // Stalled receiver + producer burst larger than the bounded queue proves the blocking
        // rejection handler applies backpressure instead of silently losing tokens — this is the
        // property SSE streaming relies on to avoid mid-stream gaps.
        List<Integer> received = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch firstReceived = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        broker.receive(TestRoute.ORDERED, message -> {
            firstReceived.countDown();

            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();
            }

            received.add((Integer) message);
        });

        int burst = 1100;
        CountDownLatch producerDone = new CountDownLatch(1);
        Thread producer = new Thread(() -> {
            for (int i = 0; i < burst; i++) {
                broker.send(TestRoute.ORDERED, i);
            }

            producerDone.countDown();
        });

        producer.start();

        assertThat(firstReceived.await(5, TimeUnit.SECONDS))
            .as("first message should start being consumed")
            .isTrue();
        assertThat(producerDone.await(1, TimeUnit.SECONDS))
            .as("producer should block on bounded queue once capacity is reached")
            .isFalse();

        release.countDown();

        assertThat(producerDone.await(10, TimeUnit.SECONDS))
            .as("producer should complete once receiver drains the queue")
            .isTrue();

        // Wait for receiver to drain fully.
        long deadline = System.currentTimeMillis() + 10_000;

        while (received.size() < burst && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }

        assertThat(received)
            .as("bounded queue must backpressure rather than drop messages")
            .hasSize(burst)
            .containsExactlyElementsOf(IntStream.range(0, burst)
                .boxed()
                .collect(Collectors.toList()));
    }

    private enum TestRoute implements MessageRoute {

        ORDERED(true, "test.ordered"),
        ORDERED_OTHER(true, "test.ordered.other"),
        UNORDERED(false, "test.unordered");

        private final boolean ordered;
        private final String routeName;

        TestRoute(boolean ordered, String routeName) {
            this.ordered = ordered;
            this.routeName = routeName;
        }

        @Override
        public Exchange getExchange() {
            return Exchange.MESSAGE;
        }

        @Override
        public String getName() {
            return routeName;
        }

        @Override
        public boolean isOrdered() {
            return ordered;
        }
    }
}
