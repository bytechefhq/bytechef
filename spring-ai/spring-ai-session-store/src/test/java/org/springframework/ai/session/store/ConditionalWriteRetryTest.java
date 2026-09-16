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

package org.springframework.ai.session.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ConditionalWriteRetryTest {

    private final List<Long> recordedDelays = new ArrayList<>();
    private ConditionalWriteRetry conditionalWriteRetry;

    @BeforeEach
    void setUp() {
        recordedDelays.clear();

        conditionalWriteRetry = new ConditionalWriteRetry(4, 10L, 25L, recordedDelays::add);
    }

    @Test
    void testReturnsTheFirstSuccessfulAttemptWithoutBackingOff() {
        AtomicInteger attempts = new AtomicInteger();

        String result = conditionalWriteRetry.call("save", "session-1", () -> {
            attempts.incrementAndGet();

            return Optional.of("saved");
        });

        assertEquals("saved", result);
        assertEquals(1, attempts.get());
        assertTrue(recordedDelays.isEmpty());
    }

    @Test
    void testBacksOffBetweenConflictingAttempts() {
        AtomicInteger attempts = new AtomicInteger();

        String result = conditionalWriteRetry.call(
            "save", "session-1", () -> attempts.incrementAndGet() < 3 ? Optional.empty() : Optional.of("saved"));

        assertEquals("saved", result);
        assertEquals(3, attempts.get());
        assertEquals(2, recordedDelays.size());
    }

    @Test
    void testThrowsAfterTheLastConflictingAttempt() {
        AtomicInteger attempts = new AtomicInteger();

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> conditionalWriteRetry.run("append an event", "session-1", () -> attempts.incrementAndGet() < 0));

        assertEquals(4, attempts.get());
        assertEquals(3, recordedDelays.size());
        assertTrue(exception.getMessage()
            .contains("session-1"));
        assertTrue(exception.getMessage()
            .contains("concurrent writers"));
    }

    @Test
    void testDefaultSleeperStopsRetryingOnAnInterruptedThreadAndKeepsTheInterruptFlag() {
        ConditionalWriteRetry defaultConditionalWriteRetry = ConditionalWriteRetry.defaults();
        AtomicInteger attempts = new AtomicInteger();

        Thread currentThread = Thread.currentThread();

        currentThread.interrupt();

        try {
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> defaultConditionalWriteRetry.run("save", "session-1", () -> attempts.incrementAndGet() < 0));

            assertTrue(currentThread.isInterrupted());
            assertEquals(1, attempts.get());
            assertInstanceOf(InterruptedException.class, exception.getCause());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void testDelaysGrowExponentiallyWithJitterUpToTheCap() {
        for (int iteration = 0; iteration < 200; iteration++) {
            long firstDelay = conditionalWriteRetry.delayMillis(1);
            long secondDelay = conditionalWriteRetry.delayMillis(2);
            long cappedDelay = conditionalWriteRetry.delayMillis(10);

            assertTrue(firstDelay >= 5L && firstDelay <= 10L, "first delay " + firstDelay);
            assertTrue(secondDelay >= 10L && secondDelay <= 20L, "second delay " + secondDelay);
            assertTrue(cappedDelay >= 12L && cappedDelay <= 25L, "capped delay " + cappedDelay);
        }
    }

    @Test
    void testRejectsInvalidSettings() {
        assertThrows(
            IllegalArgumentException.class, () -> new ConditionalWriteRetry(0, 10L, 20L, recordedDelays::add));
        assertThrows(
            IllegalArgumentException.class, () -> new ConditionalWriteRetry(3, 0L, 20L, recordedDelays::add));
        assertThrows(
            IllegalArgumentException.class, () -> new ConditionalWriteRetry(3, 30L, 20L, recordedDelays::add));
    }
}
