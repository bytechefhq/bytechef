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

package com.bytechef.platform.coordinator.notification;

import com.bytechef.platform.notification.domain.NotificationEvent;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class NotificationThrottle {

    private static final int MAXIMUM_SIZE = 10_000;

    private final Clock clock;
    private final ConcurrentMap<Key, State> states;
    private final Duration window;

    public NotificationThrottle(Duration window) {
        this(window, Clock.systemUTC());
    }

    public NotificationThrottle(Duration window, Clock clock) {
        Cache<Key, State> cache = Caffeine.newBuilder()
            .maximumSize(MAXIMUM_SIZE)
            .build();

        this.clock = clock;
        this.states = cache.asMap();
        this.window = window;
    }

    public OptionalLong tryAcquire(Long notificationId, @Nullable String workflowId, NotificationEvent.Type eventType) {
        if (window.isZero() || window.isNegative()) {
            return OptionalLong.of(0);
        }

        Instant now = clock.instant();
        AtomicLong suppressedCount = new AtomicLong(-1);

        states.compute(new Key(notificationId, workflowId, eventType), (key, state) -> {
            if (state == null || !now.isBefore(state.lastSentDate.plus(window))) {
                suppressedCount.set(state == null ? 0 : state.suppressedCount);

                return new State(now, 0);
            }

            return new State(state.lastSentDate, state.suppressedCount + 1);
        });

        long count = suppressedCount.get();

        return count < 0 ? OptionalLong.empty() : OptionalLong.of(count);
    }

    private record Key(Long notificationId, @Nullable String workflowId, NotificationEvent.Type eventType) {
    }

    private record State(Instant lastSentDate, long suppressedCount) {
    }
}
