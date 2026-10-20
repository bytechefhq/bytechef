/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class WorkflowChatTurnStopper {

    private static final Logger log = LoggerFactory.getLogger(WorkflowChatTurnStopper.class);

    private final long chatId;
    private final String tenantId;
    @Nullable
    private final JobFacade jobFacade;

    private final AtomicReference<@Nullable Long> jobIdReference = new AtomicReference<>();
    private final AtomicBoolean stopRequested = new AtomicBoolean();
    private final AtomicBoolean stopped = new AtomicBoolean();

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkflowChatTurnStopper(long chatId, String tenantId, @Nullable JobFacade jobFacade) {
        this.chatId = chatId;
        this.tenantId = tenantId;
        this.jobFacade = jobFacade;
    }

    public void onStopRequested() {
        stopRequested.set(true);

        stopIfReady();
    }

    public void onJobId(long jobId) {
        jobIdReference.compareAndSet(null, jobId);

        stopIfReady();
    }

    private void stopIfReady() {
        Long jobId = jobIdReference.get();

        if (jobFacade == null || jobId == null || !stopRequested.get() || !stopped.compareAndSet(false, true)) {
            return;
        }

        String previousTenantId = TenantContext.getCurrentTenantId();

        TenantContext.setCurrentTenantId(tenantId);

        try {
            jobFacade.stopJob(jobId);
        } catch (RuntimeException exception) {
            log.warn("Failed to stop job {} of workflow chat {}", jobId, chatId, exception);
        } finally {
            TenantContext.setCurrentTenantId(previousTenantId);
        }
    }
}
