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

package com.bytechef.platform.component.polyglot;

import java.time.Duration;
import org.jspecify.annotations.Nullable;

/**
 * Guest execution limits applied to every sandboxed polyglot context.
 *
 * <p>
 * A {@code null} limit means unlimited - it is left off the context builder entirely rather than passed as zero, which
 * GraalVM would read as "deny everything".
 *
 * @param enabled                 whether guest contexts are built under a {@link org.graalvm.polyglot.SandboxPolicy}
 *                                above {@code TRUSTED} at all; the kill switch
 * @param maxCpuTime              the CPU time a single guest execution may consume, or {@code null} for unlimited
 * @param maxHeapMemory           the heap a single guest execution may allocate, in bytes, or {@code null} for
 *                                unlimited
 * @param maxConcurrentExecutions how many guest executions may hold a platform thread at once; further executions queue
 * @author Ivica Cardic
 */
public record PolyglotSandboxSettings(
    boolean enabled, @Nullable Duration maxCpuTime, @Nullable Long maxHeapMemory, int maxConcurrentExecutions) {

    /** Five minutes - long enough that only a script that already ran away notices the ceiling. */
    public static final Duration DEFAULT_MAX_CPU_TIME = Duration.ofMinutes(5);

    /** 512 MiB. */
    public static final long DEFAULT_MAX_HEAP_MEMORY = 512L * 1024 * 1024;

    /**
     * Four per core, at least sixteen. Guest scripts mix compute with calls back out to component actions, so the
     * ceiling sits well above the core count; it exists to bound how much of the machine runaway guest code can hold,
     * not to ration ordinary use.
     */
    public static final int DEFAULT_MAX_CONCURRENT_EXECUTIONS =
        Math.max(16, Runtime.getRuntime()
            .availableProcessors() * 4);

    public PolyglotSandboxSettings {
        if (maxConcurrentExecutions < 1) {
            throw new IllegalArgumentException("maxConcurrentExecutions must be at least 1");
        }
    }

    public static PolyglotSandboxSettings defaults() {
        return new PolyglotSandboxSettings(
            true, DEFAULT_MAX_CPU_TIME, DEFAULT_MAX_HEAP_MEMORY, DEFAULT_MAX_CONCURRENT_EXECUTIONS);
    }

    /** The pre-sandbox behavior: guest contexts stay on {@code TRUSTED} with no resource ceilings. */
    public static PolyglotSandboxSettings disabled() {
        return new PolyglotSandboxSettings(false, null, null, DEFAULT_MAX_CONCURRENT_EXECUTIONS);
    }
}
