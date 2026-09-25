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
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class JobCompletionAwaiterImpl implements JobCompletionAwaiter {

    private static final Logger log = LoggerFactory.getLogger(JobCompletionAwaiterImpl.class);

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

        Optional<Job> jobOptional = jobService.fetchJob(jobId);

        if (jobOptional.isPresent() && isTerminal(jobOptional.get())) {
            future.complete(jobOptional.get());

            return future;
        }

        String tenantId = TenantContext.getCurrentTenantId();

        CompletableFuture.delayedExecutor(timeout.toMillis(), TimeUnit.MILLISECONDS)
            .execute(() -> TenantContext.runWithTenantId(tenantId, () -> completeOnTimeout(jobId, timeout, future)));

        return future;
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

        if ("COMPLETED".equals(status) || "FAILED".equals(status) || "STOPPED".equals(status)) {
            try {
                future.complete(jobService.getJob(jobId));
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        }
    }

    private void completeOnTimeout(long jobId, Duration timeout, CompletableFuture<Job> future) {
        if (future.isDone()) {
            return;
        }

        try {
            Optional<Job> jobOptional = jobService.fetchJob(jobId);

            if (jobOptional.isPresent() && isTerminal(jobOptional.get())) {
                future.complete(jobOptional.get());

                return;
            }
        } catch (RuntimeException exception) {
            log.error("Unable to read the status of job {} after waiting {}", jobId, timeout, exception);
        }

        future.completeExceptionally(
            new TimeoutException(
                "Job %d did not finish within %s".formatted(
                    jobId, timeout)));
    }

    private static boolean isTerminal(Job job) {
        Job.Status status = job.getStatus();

        return status == Job.Status.COMPLETED || status == Job.Status.FAILED || status == Job.Status.STOPPED;
    }
}
