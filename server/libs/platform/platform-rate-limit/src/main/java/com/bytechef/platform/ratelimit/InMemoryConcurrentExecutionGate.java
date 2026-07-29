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

package com.bytechef.platform.ratelimit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory, per-node {@link ConcurrentExecutionGate}: a restart resets counters to zero while previously admitted jobs
 * keep running, temporarily over-admitting until they finish — the safe direction (never wrongly blocks).
 * {@code release} floors at zero so redelivered terminal events can't push a counter negative.
 *
 * @author Ivica Cardic
 */
public class InMemoryConcurrentExecutionGate implements ConcurrentExecutionGate {

    private final ConcurrentHashMap<String, AtomicInteger> slots = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, int limit) {
        AtomicInteger counter = slots.computeIfAbsent(key, k -> new AtomicInteger());

        while (true) {
            int current = counter.get();

            if (current >= limit) {
                return false;
            }

            if (counter.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    @Override
    public void release(String key) {
        AtomicInteger counter = slots.get(key);

        if (counter == null) {
            return;
        }

        counter.updateAndGet(current -> Math.max(0, current - 1));
    }

    @Override
    public int held(String key) {
        AtomicInteger counter = slots.get(key);

        return counter == null ? 0 : counter.get();
    }
}
