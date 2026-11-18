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
 * @author Ivica Cardic
 */
public record PolyglotSandboxSettings(
    boolean enabled, @Nullable Duration maxCpuTime, @Nullable Long maxHeapMemory, int maxConcurrentExecutions) {

    public static final Duration DEFAULT_MAX_CPU_TIME = Duration.ofMinutes(5);

    public static final long DEFAULT_MAX_HEAP_MEMORY = 512L * 1024 * 1024;

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

    public static PolyglotSandboxSettings disabled() {
        return new PolyglotSandboxSettings(false, null, null, DEFAULT_MAX_CONCURRENT_EXECUTIONS);
    }
}
