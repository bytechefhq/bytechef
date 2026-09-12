/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.job.listener;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class JobStatusApplicationEventListener implements ApplicationEventListener, ExitCodeGenerator {

    private static final int EXIT_CODE_FAILURE = 1;
    private static final int EXIT_CODE_SUCCESS = 0;

    private static final Set<Job.Status> TERMINAL_STATUSES =
        EnumSet.of(Job.Status.COMPLETED, Job.Status.FAILED, Job.Status.STOPPED);

    private static final Logger log = LoggerFactory.getLogger(JobStatusApplicationEventListener.class);

    private final AtomicInteger exitCode = new AtomicInteger(EXIT_CODE_SUCCESS);
    private final Object monitor = new Object();
    private final Map<Long, Job.Status> terminalStatuses = new HashMap<>();

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (!(applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent)) {
            return;
        }

        Job.Status status = jobStatusApplicationEvent.getStatus();

        if (!TERMINAL_STATUSES.contains(status)) {
            return;
        }

        long jobId = jobStatusApplicationEvent.getJobId();

        log.info("Job id={} finished with status={}", jobId, status);

        synchronized (monitor) {
            terminalStatuses.put(jobId, status);

            monitor.notifyAll();
        }
    }

    public void awaitTermination(long jobId, @Nullable Duration timeout) throws InterruptedException {
        long deadline = timeout == null ? 0 : System.currentTimeMillis() + timeout.toMillis();

        Job.Status status;

        synchronized (monitor) {
            while (!terminalStatuses.containsKey(jobId)) {
                if (timeout == null) {
                    monitor.wait();

                    continue;
                }

                long remaining = deadline - System.currentTimeMillis();

                if (remaining <= 0) {
                    log.error("Job id={} did not reach a terminal status within {}, giving up", jobId, timeout);

                    exitCode.set(EXIT_CODE_FAILURE);

                    return;
                }

                monitor.wait(remaining);
            }

            status = terminalStatuses.get(jobId);
        }

        exitCode.set(status == Job.Status.COMPLETED ? EXIT_CODE_SUCCESS : EXIT_CODE_FAILURE);
    }

    @Override
    public int getExitCode() {
        return exitCode.get();
    }
}
