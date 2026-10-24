/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.tenant.TenantContext;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkflowChatTurnStopperTest {

    private static final long CHAT_ID = 11L;
    private static final String TURN_TENANT_ID = "acme";

    private JobFacade jobFacade;
    private WorkflowChatTurnStopper stopper;

    @BeforeEach
    void setUp() {
        TenantContext.resetCurrentTenantId();

        jobFacade = mock(JobFacade.class);
        stopper = new WorkflowChatTurnStopper(CHAT_ID, TURN_TENANT_ID, jobFacade::stopJob);
    }

    @AfterEach
    void tearDown() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testStopBeforeJobIdStopsTheJobWhenItArrives() {
        stopper.onStopRequested();

        verifyNoInteractions(jobFacade);

        stopper.onJobId(77L);

        verify(jobFacade).stopJob(77L);
    }

    @Test
    void testStopAfterJobIdStopsAtOnceAndOnlyOnce() {
        stopper.onJobId(77L);
        stopper.onStopRequested();
        stopper.onStopRequested();

        verify(jobFacade, times(1)).stopJob(77L);
    }

    @Test
    void testRepeatedJobIdsAfterTheStopDoNotStopAgain() {
        stopper.onStopRequested();
        stopper.onJobId(77L);
        stopper.onJobId(78L);

        verify(jobFacade, times(1)).stopJob(77L);
        verify(jobFacade, never()).stopJob(78L);
    }

    @Test
    void testJobIdWithoutStopDoesNothing() {
        stopper.onJobId(77L);

        verifyNoInteractions(jobFacade);
    }

    @Test
    void testWithoutJobFacadeBothSignalsAreANoOp() {
        WorkflowChatTurnStopper stopperWithoutJobFacade = new WorkflowChatTurnStopper(CHAT_ID, TURN_TENANT_ID, null);

        assertThatNoException().isThrownBy(() -> {
            stopperWithoutJobFacade.onStopRequested();
            stopperWithoutJobFacade.onJobId(77L);
        });
    }

    @Test
    void testAFailingStopJobDoesNotThrowBackIntoTheCaller() {
        doThrow(new IllegalStateException("job store down")).when(jobFacade)
            .stopJob(77L);

        stopper.onStopRequested();

        assertThatNoException().isThrownBy(() -> stopper.onJobId(77L));

        verify(jobFacade).stopJob(77L);
    }

    @Test
    void testStopJobRunsUnderTheTurnsTenantAndRestoresTheCallersTenant() {
        AtomicReference<String> stopJobTenantId = new AtomicReference<>();

        doAnswer(invocation -> {
            stopJobTenantId.set(TenantContext.getCurrentTenantId());

            return null;
        }).when(jobFacade)
            .stopJob(77L);

        TenantContext.setCurrentTenantId("other");

        stopper.onStopRequested();
        stopper.onJobId(77L);

        assertThat(stopJobTenantId.get()).isEqualTo(TURN_TENANT_ID);
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo("other");
    }
}
