/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.job.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class JobStatusApplicationEventListenerTest {

    private JobStatusApplicationEventListener jobStatusApplicationEventListener;

    @BeforeEach
    void beforeEach() {
        jobStatusApplicationEventListener = new JobStatusApplicationEventListener();
    }

    @Test
    void testExitCodeIsSuccessOnCompletedStatus() throws InterruptedException {
        jobStatusApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(1L, Job.Status.COMPLETED));

        jobStatusApplicationEventListener.awaitTermination(1L, null);

        assertEquals(0, jobStatusApplicationEventListener.getExitCode());
    }

    @Test
    void testExitCodeIsFailureOnFailedStatus() throws InterruptedException {
        jobStatusApplicationEventListener.onApplicationEvent(new JobStatusApplicationEvent(1L, Job.Status.FAILED));

        jobStatusApplicationEventListener.awaitTermination(1L, null);

        assertEquals(1, jobStatusApplicationEventListener.getExitCode());
    }

    @Test
    void testExitCodeIsFailureOnStoppedStatus() throws InterruptedException {
        jobStatusApplicationEventListener.onApplicationEvent(new JobStatusApplicationEvent(1L, Job.Status.STOPPED));

        jobStatusApplicationEventListener.awaitTermination(1L, null);

        assertEquals(1, jobStatusApplicationEventListener.getExitCode());
    }

    @Test
    void testExitCodeIsFailureWhenTimeoutElapses() throws InterruptedException {
        jobStatusApplicationEventListener.awaitTermination(1L, Duration.ofMillis(50));

        assertEquals(1, jobStatusApplicationEventListener.getExitCode());
    }

    /**
     * A subflow publishes a terminal event for its child job before the parent continues, so a terminal status for any
     * job other than the one being waited on must not release the runner.
     */
    @Test
    void testTerminalStatusOfAnotherJobDoesNotTerminate() throws InterruptedException {
        jobStatusApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(2L, Job.Status.COMPLETED));

        jobStatusApplicationEventListener.awaitTermination(1L, Duration.ofMillis(50));

        assertEquals(1, jobStatusApplicationEventListener.getExitCode());
    }

    @Test
    void testTerminalStatusRecordedBeforeAwaitIsNotMissed() throws InterruptedException {
        jobStatusApplicationEventListener.onApplicationEvent(
            new JobStatusApplicationEvent(1L, Job.Status.COMPLETED));

        jobStatusApplicationEventListener.awaitTermination(1L, Duration.ofMillis(50));

        assertEquals(0, jobStatusApplicationEventListener.getExitCode());
    }

    @Test
    void testNonTerminalStatusDoesNotTerminate() throws InterruptedException {
        jobStatusApplicationEventListener.onApplicationEvent(new JobStatusApplicationEvent(1L, Job.Status.CREATED));
        jobStatusApplicationEventListener.onApplicationEvent(new JobStatusApplicationEvent(1L, Job.Status.STARTED));

        jobStatusApplicationEventListener.awaitTermination(1L, Duration.ofMillis(50));

        assertEquals(1, jobStatusApplicationEventListener.getExitCode());
    }
}
