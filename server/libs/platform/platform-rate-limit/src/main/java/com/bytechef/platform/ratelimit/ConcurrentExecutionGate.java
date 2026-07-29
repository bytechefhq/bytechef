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

/**
 * Per-tenant concurrent-execution slots (Sim model: a slot is held from job admission until the job reaches terminal
 * status). Acquired at the {@code PrincipalJobFacade} admission point, released by the terminal-status listener in
 * platform-coordinator — the engine under {@code server/libs/atlas/} stays untouched. Implementations:
 * {@link InMemoryConcurrentExecutionGate} (default, per-node) and {@link RedisConcurrentExecutionGate} (strict global
 * limits, {@code bytechef.plan.enforcement.provider=redis}).
 *
 * @author Ivica Cardic
 */
public interface ConcurrentExecutionGate {

    /** Acquires a slot for {@code key} unless {@code limit} slots are already held. */
    boolean tryAcquire(String key, int limit);

    /** Releases one slot for {@code key}, flooring at zero so redelivered terminal events are harmless. */
    void release(String key);

    /** Returns the number of slots currently held for {@code key}. */
    int held(String key);
}
