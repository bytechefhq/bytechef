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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Ivica Cardic
 */
class AsyncMessageBrokerTest {

    @Test
    void orderedRoutePreservesFifoUnderConcurrentSends() throws InterruptedException {
        AsyncMessageBroker broker = new AsyncMessageBroker(new MockEnvironment());
        List<Integer> received = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
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
        AsyncMessageBroker broker = new AsyncMessageBroker(new MockEnvironment());
        java.util.Set<String> dispatchThreads = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
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
        AsyncMessageBroker broker = new AsyncMessageBroker(new MockEnvironment());
        AtomicInteger routeAThread = new AtomicInteger();
        AtomicInteger routeBThread = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(2);
        List<String> routeAThreadNames = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        List<String> routeBThreadNames = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        broker.receive(TestRoute.ORDERED, message -> {
            Thread currentThread = Thread.currentThread();

            routeAThreadNames.add(currentThread.getName());
            routeAThread.incrementAndGet();

            latch.countDown();
        });

        broker.receive(TestRoute.ORDERED_OTHER, message -> {
            Thread currentThread = Thread.currentThread();

            routeBThreadNames.add(currentThread.getName());
            routeBThread.incrementAndGet();

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
        AsyncMessageBroker broker = new AsyncMessageBroker(new MockEnvironment());
        List<Integer> received = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
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
