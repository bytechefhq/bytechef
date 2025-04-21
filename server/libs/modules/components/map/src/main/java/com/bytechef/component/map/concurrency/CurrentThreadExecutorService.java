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

package com.bytechef.component.map.concurrency;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Ivica Cardic
 * @author The Guava Authors
 */
public class CurrentThreadExecutorService extends AbstractExecutorService {

    /** Lock used whenever accessing the state variables (runningTasks, shutdown) of the executor */
    private final Object lock = new Object();

    /*
     * Conceptually, these two variables describe the executor being in one of three states: - Active: shutdown == false
     * - Shutdown: runningTasks > 0 and shutdown == true - Terminated: runningTasks == 0 and shutdown == true
     */
    private int runningTasks = 0;
    private boolean shutdown = false;

    @Override
    public void execute(Runnable command) {
        startTask();

        try {
            command.run();
        } finally {
            endTask();
        }
    }

    @Override
    public boolean isShutdown() {
        synchronized (lock) {
            return shutdown;
        }
    }

    @Override
    public void shutdown() {
        synchronized (lock) {
            shutdown = true;

            if (runningTasks == 0) {
                lock.notifyAll();
            }
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown();

        return Collections.emptyList();
    }

    @Override
    public boolean isTerminated() {
        synchronized (lock) {
            return shutdown && runningTasks == 0;
        }
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);

        synchronized (lock) {
            while (true) {
                if (shutdown && runningTasks == 0) {
                    return true;
                } else if (nanos <= 0) {
                    return false;
                } else {
                    long now = System.nanoTime();

                    TimeUnit.NANOSECONDS.timedWait(lock, nanos);
                    nanos -= System.nanoTime() - now; // subtract the actual time we waited
                }
            }
        }
    }

    /**
     * Checks if the executor has been shut down and increments the running task count.
     *
     * @throws RejectedExecutionException if the executor has been previously shutdown
     */
    private void startTask() {
        synchronized (lock) {
            if (shutdown) {
                throw new RejectedExecutionException("Executor already shutdown");
            }

            runningTasks++;
        }
    }

    /** Decrements the running task count. */
    private void endTask() {
        synchronized (lock) {
            int numRunning = --runningTasks;

            if (numRunning == 0) {
                lock.notifyAll();
            }
        }
    }
}
