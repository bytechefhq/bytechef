/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.runtime.job.executor.JobRunner;
import com.bytechef.runtime.job.listener.JobStatusApplicationEventListener;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class RuntimeJobApplicationTest {

    private ApplicationArguments applicationArguments;
    private JobRunner jobRunner;
    private JobStatusApplicationEventListener jobStatusApplicationEventListener;
    private RuntimeJobApplication runtimeJobApplication;

    @BeforeEach
    void beforeEach() {
        applicationArguments = mock(ApplicationArguments.class);
        jobRunner = mock(JobRunner.class);
        jobStatusApplicationEventListener = new JobStatusApplicationEventListener();

        when(applicationArguments.getOptionValues("workflow")).thenReturn(List.of("/workflows/workflow1.json"));

        runtimeJobApplication = new RuntimeJobApplication(jobRunner, jobStatusApplicationEventListener);
    }

    /**
     * Job execution is fully asynchronous, so the runner must not return until the job reaches a terminal status.
     * Returning early lets Spring Boot close the application context while tasks are still being dispatched, which
     * makes every remaining {@code @Async} hop fail with "SimpleAsyncTaskExecutor is not active".
     */
    @Test
    void testRunBlocksUntilJobReachesTerminalStatus() throws InterruptedException {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch jobSubmitted = new CountDownLatch(1);
        CountDownLatch runReturned = new CountDownLatch(1);

        doAnswer(invocation -> {
            jobSubmitted.countDown();

            return 1L;
        }).when(jobRunner)
            .run(anyString(), any());

        Thread thread = Thread.ofVirtual()
            .start(() -> {
                try {
                    runtimeJobApplication.run(applicationArguments);
                } catch (Throwable throwable) {
                    failure.set(throwable);
                }

                runReturned.countDown();
            });

        assertTrue(jobSubmitted.await(5, TimeUnit.SECONDS), "the job was never submitted");
        assertFalse(
            runReturned.await(200, TimeUnit.MILLISECONDS), "run() returned before the job reached a terminal status");

        jobStatusApplicationEventListener.onApplicationEvent(new JobStatusApplicationEvent(1L, Job.Status.COMPLETED));

        assertTrue(runReturned.await(5, TimeUnit.SECONDS), "run() did not return after the job completed");

        thread.join();

        assertNull(failure.get(), () -> "run() failed: " + failure.get());
        assertEquals(0, jobStatusApplicationEventListener.getExitCode());
    }

    @Test
    void testRunGivesUpOnceTheTimeoutElapses() throws InterruptedException {
        when(applicationArguments.getOptionValues("timeout")).thenReturn(List.of("100ms"));

        runtimeJobApplication.run(applicationArguments);

        assertEquals(1, jobStatusApplicationEventListener.getExitCode());
    }

    /**
     * An unparseable argument must be rejected before anything is submitted. Submitting first would start a workflow
     * and then tear the context down around it.
     */
    @Test
    void testRunRejectsAnInvalidTimeoutBeforeSubmittingTheJob() {
        when(applicationArguments.getOptionValues("timeout")).thenReturn(List.of("bogus"));

        assertThrows(IllegalArgumentException.class, () -> runtimeJobApplication.run(applicationArguments));

        verifyNoInteractions(jobRunner);
    }

    /**
     * {@code ApplicationArguments} returns an empty list for an option passed without a value, such as
     * {@code --parameters}, so the runner must not blindly take the first element.
     */
    @Test
    void testRunAcceptsOptionsPassedWithoutAValue() throws InterruptedException {
        when(applicationArguments.getOptionValues("parameters")).thenReturn(List.of());
        when(applicationArguments.getOptionValues("connections")).thenReturn(List.of());
        when(jobRunner.run(anyString(), any())).thenReturn(1L);

        jobStatusApplicationEventListener.onApplicationEvent(new JobStatusApplicationEvent(1L, Job.Status.COMPLETED));

        runtimeJobApplication.run(applicationArguments);

        verify(jobRunner).run("/workflows/workflow1.json", Map.of());
    }
}
