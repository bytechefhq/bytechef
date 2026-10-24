/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongConsumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stops the workflow job of one workflow chat turn when the turn is stopped, either by the user (from
 * {@link InFlightAiHubRunRegistry#stopRequested}) or by a stream error. The two inputs arrive independently and in
 * either order: the stop and the job id (from the workflow's {@code start} event, or reported by a synchronous
 * execution as soon as its job exists). The stop action runs exactly once, as soon as both have happened, only for the
 * job id this turn was handed, and under the turn's tenant, because both inputs are delivered on threads that do not
 * carry it. The stop action depends on how the job runs: the job facade for a streamed job, the synchronous executor
 * for a synchronous one. Both methods are idempotent and never throw, so they are safe to call from the workflow's
 * event stream. Without a stop action both are no-ops.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class WorkflowChatTurnStopper {

    private static final Logger log = LoggerFactory.getLogger(WorkflowChatTurnStopper.class);

    private final long chatId;
    private final String tenantId;
    @Nullable
    private final LongConsumer jobStopAction;

    private final AtomicReference<@Nullable Long> jobIdReference = new AtomicReference<>();
    private final AtomicBoolean stopRequested = new AtomicBoolean();
    private final AtomicBoolean stopped = new AtomicBoolean();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowChatTurnStopper(long chatId, String tenantId, @Nullable LongConsumer jobStopAction) {
        this.chatId = chatId;
        this.tenantId = tenantId;
        this.jobStopAction = jobStopAction;
    }

    /**
     * Records that the turn was stopped, and stops the job when its id is already known.
     */
    public void onStopRequested() {
        stopRequested.set(true);

        stopIfReady();
    }

    /**
     * Records the turn's job id (the first one wins), and stops the job when the turn was already stopped.
     */
    public void onJobId(long jobId) {
        jobIdReference.compareAndSet(null, jobId);

        stopIfReady();
    }

    private void stopIfReady() {
        Long jobId = jobIdReference.get();

        if (jobStopAction == null || jobId == null || !stopRequested.get() || !stopped.compareAndSet(false, true)) {
            return;
        }

        String previousTenantId = TenantContext.getCurrentTenantId();

        TenantContext.setCurrentTenantId(tenantId);

        try {
            jobStopAction.accept(jobId);
        } catch (RuntimeException exception) {
            log.warn("Failed to stop job {} of workflow chat {}", jobId, chatId, exception);
        } finally {
            TenantContext.setCurrentTenantId(previousTenantId);
        }
    }
}
