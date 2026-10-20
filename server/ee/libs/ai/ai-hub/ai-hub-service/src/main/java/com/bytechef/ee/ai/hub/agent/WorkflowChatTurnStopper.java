/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.bytechef.atlas.execution.facade.JobFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stops the workflow job of one workflow chat turn when the user stops the turn. The two inputs arrive independently
 * and in either order: the Stop (from {@link InFlightAiHubRunRegistry#stopRequested}) and the job id (from the
 * workflow's {@code start} event). {@link JobFacade#stopJob} runs exactly once, as soon as both have happened, and the
 * chat's entry in {@link WorkflowChatJobRegistry} is cleared after it. Both methods are idempotent and never throw, so
 * they are safe to call from the workflow's event stream. Without a {@link JobFacade} both are no-ops.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class WorkflowChatTurnStopper {

    private static final Logger log = LoggerFactory.getLogger(WorkflowChatTurnStopper.class);

    private final long chatId;
    private final WorkflowChatJobRegistry jobRegistry;
    @Nullable
    private final JobFacade jobFacade;

    private final AtomicReference<@Nullable Long> jobIdReference = new AtomicReference<>();
    private final AtomicBoolean stopRequested = new AtomicBoolean();
    private final AtomicBoolean stopped = new AtomicBoolean();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowChatTurnStopper(long chatId, WorkflowChatJobRegistry jobRegistry, @Nullable JobFacade jobFacade) {
        this.chatId = chatId;
        this.jobRegistry = jobRegistry;
        this.jobFacade = jobFacade;
    }

    /**
     * Records that the user stopped the turn, and stops the job when its id is already known.
     */
    public void onStopRequested() {
        stopRequested.set(true);

        stopIfReady();
    }

    /**
     * Records the turn's job id (the first one wins), and stops the job when the user already stopped the turn.
     */
    public void onJobId(long jobId) {
        jobIdReference.compareAndSet(null, jobId);

        stopIfReady();
    }

    private void stopIfReady() {
        Long jobId = jobIdReference.get();

        if (jobFacade == null || jobId == null || !stopRequested.get() || !stopped.compareAndSet(false, true)) {
            return;
        }

        try {
            jobFacade.stopJob(jobId);

            jobRegistry.clear(chatId);
        } catch (RuntimeException exception) {
            log.warn("Failed to stop job {} of workflow chat {}", jobId, chatId, exception);
        }
    }
}
