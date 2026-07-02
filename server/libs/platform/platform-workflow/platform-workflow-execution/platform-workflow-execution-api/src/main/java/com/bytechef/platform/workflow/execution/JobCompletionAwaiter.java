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

package com.bytechef.platform.workflow.execution;

import com.bytechef.atlas.execution.domain.Job;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Awaits the terminal status of a distributed-coordinator job. The completion signal is the broker-published
 * {@code SSE_STREAM_EVENTS} job-status event (the only completion signal that crosses process boundaries), so this
 * replaces the embedded {@code JobSyncExecutor}'s in-process latch for synchronous webhook and MCP-tool execution.
 *
 * @author Ivica Cardic
 */
public interface JobCompletionAwaiter {

    /**
     * Default maximum wait for a synchronous, request-scoped job — webhook sync execution and MCP tool invocation —
     * before {@link #await(long, Duration)} times out. Shared by all synchronous callers so the bound cannot drift
     * between paths.
     */
    Duration DEFAULT_SYNC_TIMEOUT = Duration.ofSeconds(300);

    /**
     * Returns a future that completes with the job once it reaches a terminal status ({@code COMPLETED},
     * {@code FAILED}, or {@code STOPPED}; suspend maps to {@code STOPPED}). Completes exceptionally with
     * {@link java.util.concurrent.TimeoutException} if {@code timeout} elapses first.
     *
     * @param jobId   the id of the job to await
     * @param timeout the maximum time to wait for the job to reach a terminal status
     * @return a future resolving with the completed job
     */
    CompletableFuture<Job> await(long jobId, Duration timeout);
}
