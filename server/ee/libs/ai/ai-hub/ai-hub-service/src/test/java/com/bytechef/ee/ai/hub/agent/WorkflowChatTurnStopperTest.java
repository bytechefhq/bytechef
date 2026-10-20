/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.atlas.execution.facade.JobFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkflowChatTurnStopperTest {

    private static final long CHAT_ID = 11L;

    private JobFacade jobFacade;
    private WorkflowChatJobRegistry jobRegistry;
    private WorkflowChatTurnStopper stopper;

    @BeforeEach
    void setUp() {
        jobFacade = mock(JobFacade.class);
        jobRegistry = mock(WorkflowChatJobRegistry.class);
        stopper = new WorkflowChatTurnStopper(CHAT_ID, jobRegistry, jobFacade);
    }

    @Test
    void testStopBeforeJobIdStopsTheJobWhenItArrives() {
        stopper.onStopRequested();

        verifyNoInteractions(jobFacade);

        stopper.onJobId(77L);

        verify(jobFacade).stopJob(77L);
        verify(jobRegistry).clear(CHAT_ID);
    }

    @Test
    void testStopAfterJobIdStopsAtOnceAndOnlyOnce() {
        stopper.onJobId(77L);
        stopper.onStopRequested();
        stopper.onStopRequested();

        verify(jobFacade, times(1)).stopJob(77L);
        verify(jobRegistry, times(1)).clear(CHAT_ID);
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

        verifyNoInteractions(jobFacade, jobRegistry);
    }

    @Test
    void testWithoutJobFacadeBothSignalsAreANoOp() {
        WorkflowChatTurnStopper stopperWithoutJobFacade = new WorkflowChatTurnStopper(CHAT_ID, jobRegistry, null);

        stopperWithoutJobFacade.onStopRequested();
        stopperWithoutJobFacade.onJobId(77L);

        verifyNoInteractions(jobRegistry);
    }

    @Test
    void testAFailingStopJobDoesNotThrowBackIntoTheCaller() {
        doThrow(new IllegalStateException("job store down")).when(jobFacade)
            .stopJob(77L);

        stopper.onStopRequested();

        assertThatNoException().isThrownBy(() -> stopper.onJobId(77L));

        verify(jobFacade).stopJob(77L);
        verify(jobRegistry, never()).clear(CHAT_ID);
    }
}
