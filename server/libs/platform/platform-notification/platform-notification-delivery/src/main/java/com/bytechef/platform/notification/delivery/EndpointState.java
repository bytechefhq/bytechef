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

package com.bytechef.platform.notification.delivery;

import io.micrometer.context.ContextSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
final class EndpointState {

    static final Duration CONSECUTIVE_FAILURES_RETENTION = Duration.ofHours(1);
    static final int MAX_CONSECUTIVE_FAILURES = 10;
    static final Duration PAUSE_DURATION = Duration.ofMinutes(5);
    static final Duration PRUNE_INTERVAL = Duration.ofMinutes(1);

    private final Map<EndpointKey, ConsecutiveFailures> consecutiveFailures = new ConcurrentHashMap<>();
    private final Map<EndpointKey, EndpointQueue> endpointQueues = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> nextPruneInstant = new AtomicReference<>(Instant.MIN);
    private final Map<EndpointKey, Instant> pausedUntilInstants = new ConcurrentHashMap<>();
    private final Map<EndpointKey, Instant> skipLoggedPausedUntilInstants = new ConcurrentHashMap<>();

    List<QueuedDelivery> abandon(EndpointKey endpointKey) {
        List<QueuedDelivery> abandonedDeliveries = new ArrayList<>();

        endpointQueues.computeIfPresent(endpointKey, (key, endpointQueue) -> {
            abandonedDeliveries.addAll(endpointQueue.queuedDeliveries);

            endpointQueue.queuedDeliveries.clear();

            endpointQueue.draining = false;

            int pendingCount = endpointQueue.pendingCount.addAndGet(-abandonedDeliveries.size());

            return pendingCount > 0 ? endpointQueue : null;
        });

        return abandonedDeliveries;
    }

    void clear() {
        consecutiveFailures.clear();
        nextPruneInstant.set(Instant.MIN);
        pausedUntilInstants.clear();
        skipLoggedPausedUntilInstants.clear();
    }

    int countFailure(EndpointKey endpointKey, Instant now) {
        ConsecutiveFailures currentConsecutiveFailures = consecutiveFailures.merge(
            endpointKey, new ConsecutiveFailures(1, now),
            (existingFailures, newFailure) -> new ConsecutiveFailures(
                existingFailures.count() + 1, newFailure.lastFailedAt()));

        return currentConsecutiveFailures.count();
    }

    Admission enqueue(EndpointKey endpointKey, QueuedDelivery queuedDelivery, int maxPendingDeliveries) {
        AtomicReference<Admission> admission = new AtomicReference<>(Admission.REJECTED);

        endpointQueues.compute(endpointKey, (key, endpointQueue) -> {
            EndpointQueue currentEndpointQueue = endpointQueue == null ? new EndpointQueue() : endpointQueue;

            if (currentEndpointQueue.pendingCount.get() >= maxPendingDeliveries) {
                return endpointQueue;
            }

            currentEndpointQueue.queuedDeliveries.addLast(queuedDelivery);
            currentEndpointQueue.pendingCount.incrementAndGet();

            if (currentEndpointQueue.draining) {
                admission.set(Admission.QUEUED);
            } else {
                currentEndpointQueue.draining = true;

                admission.set(Admission.START_DRAINER);
            }

            return currentEndpointQueue;
        });

        return admission.get();
    }

    @Nullable
    Instant getPausedUntil(EndpointKey endpointKey, Instant now) {
        Instant pausedUntil = pausedUntilInstants.get(endpointKey);

        if (pausedUntil == null) {
            return null;
        }

        if (now.isBefore(pausedUntil)) {
            return pausedUntil;
        }

        pausedUntilInstants.remove(endpointKey, pausedUntil);

        return null;
    }

    int getPendingDeliveryCount() {
        int pendingDeliveryCount = 0;

        for (EndpointQueue endpointQueue : endpointQueues.values()) {
            pendingDeliveryCount += endpointQueue.pendingCount.get();
        }

        return pendingDeliveryCount;
    }

    boolean markSkipLogged(EndpointKey endpointKey, Instant pausedUntil) {
        return !pausedUntil.equals(skipLoggedPausedUntilInstants.put(endpointKey, pausedUntil));
    }

    Instant pause(EndpointKey endpointKey, Instant now) {
        Instant pausedUntil = now.plus(PAUSE_DURATION);

        pausedUntilInstants.put(endpointKey, pausedUntil);
        consecutiveFailures.remove(endpointKey);

        return pausedUntil;
    }

    void prune(Instant now) {
        Instant currentNextPruneInstant = nextPruneInstant.get();

        if (now.isBefore(currentNextPruneInstant) ||
            !nextPruneInstant.compareAndSet(currentNextPruneInstant, now.plus(PRUNE_INTERVAL))) {

            return;
        }

        Instant consecutiveFailuresExpiryInstant = now.minus(CONSECUTIVE_FAILURES_RETENTION);

        Collection<ConsecutiveFailures> consecutiveFailuresValues = consecutiveFailures.values();

        consecutiveFailuresValues.removeIf(
            failures -> !failures.lastFailedAt()
                .isAfter(consecutiveFailuresExpiryInstant));

        Collection<Instant> pausedUntilValues = pausedUntilInstants.values();

        pausedUntilValues.removeIf(pausedUntil -> !now.isBefore(pausedUntil));

        Collection<Instant> skipLoggedPausedUntilValues = skipLoggedPausedUntilInstants.values();

        skipLoggedPausedUntilValues.removeIf(pausedUntil -> !now.isBefore(pausedUntil));
    }

    void resetFailures(EndpointKey endpointKey) {
        consecutiveFailures.remove(endpointKey);
    }

    void finishRunning(EndpointKey endpointKey) {
        endpointQueues.computeIfPresent(endpointKey, (key, endpointQueue) -> {
            int pendingCount = endpointQueue.pendingCount.decrementAndGet();

            return endpointQueue.draining || pendingCount > 0 ? endpointQueue : null;
        });
    }

    @Nullable
    QueuedDelivery takeNext(EndpointKey endpointKey) {
        AtomicReference<QueuedDelivery> nextDelivery = new AtomicReference<>();

        endpointQueues.computeIfPresent(endpointKey, (key, endpointQueue) -> {
            QueuedDelivery queuedDelivery = endpointQueue.queuedDeliveries.pollFirst();

            if (queuedDelivery != null) {
                nextDelivery.set(queuedDelivery);

                return endpointQueue;
            }

            endpointQueue.draining = false;

            return endpointQueue.pendingCount.get() > 0 ? endpointQueue : null;
        });

        return nextDelivery.get();
    }

    enum Admission {

        QUEUED, REJECTED, START_DRAINER
    }

    record EndpointKey(String tenantId, String url) {
    }

    record QueuedDelivery(
        WebhookDelivery delivery, CompletableFuture<WebhookDeliveryOutcome> outcomeFuture,
        ContextSnapshot contextSnapshot) {
    }

    private static final class EndpointQueue {

        private boolean draining;
        private final AtomicInteger pendingCount = new AtomicInteger();
        private final Deque<QueuedDelivery> queuedDeliveries = new ArrayDeque<>();
    }

    private record ConsecutiveFailures(int count, Instant lastFailedAt) {
    }
}
