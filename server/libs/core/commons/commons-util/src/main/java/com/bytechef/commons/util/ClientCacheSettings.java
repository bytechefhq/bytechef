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

package com.bytechef.commons.util;

import com.github.benmanes.caffeine.cache.Scheduler;
import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * @author Ivica Cardic
 */
public record ClientCacheSettings(
    long maximumSize, Ticker ticker, Scheduler scheduler, Executor executor, Executor sizeEvictionCloseExecutor) {

    public static final Duration IDLE_TIMEOUT = Duration.ofHours(24);
    public static final long MAXIMUM_SIZE = 100;
    public static final Duration SIZE_EVICTION_CLOSE_DELAY = Duration.ofMinutes(15);

    public static ClientCacheSettings defaults() {
        return new ClientCacheSettings(
            MAXIMUM_SIZE, Ticker.systemTicker(), Scheduler.systemScheduler(), ForkJoinPool.commonPool(),
            CompletableFuture.delayedExecutor(SIZE_EVICTION_CLOSE_DELAY.toMillis(), TimeUnit.MILLISECONDS));
    }
}
