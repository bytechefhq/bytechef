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

package com.bytechef.platform.webhook.executor;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.webhook.event.SseStreamEvent;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Completes per-job futures from the broker-published {@code SSE_STREAM_EVENTS} job-status signal, so synchronous
 * callers can await a distributed-coordinator job without the embedded {@code JobSyncExecutor}. The signal is the only
 * job-completion notification that crosses process boundaries (a microservices coordinator runs in a different JVM than
 * the awaiting node), which is why this listens to the SSE stream route rather than the in-JVM
 * {@code JobStatusApplicationEvent}.
 *
 * <p>
 * Lives in {@code platform-webhook-impl} (alongside its primary consumer {@code WebhookWorkflowExecutorImpl} and the
 * sibling {@code SseStreamBridgeRegistry}) rather than {@code platform-workflow-execution-service}, so that lightweight
 * EE apps which depend on {@code platform-workflow-execution-remote-client} (e.g. {@code webhook-app}) still obtain the
 * bean: the awaiter is a local, broker-fed primitive that must run in-process wherever {@code await} is called, not a
 * remote service stub.
 *
 * @author Ivica Cardic
 */
public class JobCompletionAwaiterImpl implements JobCompletionAwaiter {

    private final Cache<String, CompletableFuture<Job>> futures = Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .build();
    private final JobService jobService;

    @SuppressFBWarnings("EI")
    public JobCompletionAwaiterImpl(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public CompletableFuture<Job> await(long jobId, Duration timeout) {
        String key = TenantCacheKeyUtils.getKey(jobId);

        CompletableFuture<Job> future = futures.get(key, cacheKey -> new CompletableFuture<>());

        future.whenComplete((job, throwable) -> futures.invalidate(key));

        // Race guard: createJob dispatches asynchronously, so a fast job can terminate before this future is
        // registered, in which case the job-status broker event was dropped (no future present). Complete the future
        // immediately if the job has already reached a terminal status.
        Optional<Job> jobOptional = jobService.fetchJob(jobId);

        if (jobOptional.isPresent() && isTerminal(jobOptional.get())) {
            future.complete(jobOptional.get());

            return future;
        }

        return future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void onSseStreamEvent(SseStreamEvent sseStreamEvent) {
        if (!SseStreamEvent.EVENT_TYPE_JOB_STATUS.equals(sseStreamEvent.getEventType())) {
            return;
        }

        long jobId = sseStreamEvent.getJobId();

        CompletableFuture<Job> future = futures.getIfPresent(TenantCacheKeyUtils.getKey(jobId));

        if (future == null) {
            return;
        }

        Object payload = sseStreamEvent.getPayload();
        String status = payload != null ? payload.toString() : "";

        if ("COMPLETED".equals(status) || "FAILED".equals(status) || "STOPPED".equals(status)
            || "CANCELLED".equals(status)) {
            try {
                future.complete(jobService.getJob(jobId));
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        }
    }

    private static boolean isTerminal(Job job) {
        Job.Status status = job.getStatus();

        return status == Job.Status.COMPLETED || status == Job.Status.FAILED || status == Job.Status.STOPPED
            || status == Job.Status.CANCELLED;
    }
}
