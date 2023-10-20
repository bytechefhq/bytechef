
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.worker;

import com.bytechef.event.EventPublisher;
import com.bytechef.hermes.event.TriggerStartedWorkflowEvent;
import com.bytechef.message.broker.SystemMessageRoute;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.commons.util.ExceptionUtils;
import com.bytechef.error.ExecutionError;
import com.bytechef.hermes.message.broker.TriggerMessageRoute;
import com.bytechef.hermes.trigger.ControlTrigger;
import com.bytechef.hermes.trigger.TriggerExecution;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Ivica Cardic
 */
public class TriggerWorker {

    private static final Logger logger = LoggerFactory.getLogger(TriggerWorker.class);

    private static final long DEFAULT_TIME_OUT = 24 * 60 * 60 * 1000; // 24 hours

    private final EventPublisher eventPublisher;
    private final ExecutorService executorService;
    private final MessageBroker messageBroker;
    private final Map<WorkflowExecutionId, TriggerExecutionFuture<?>> triggerExecutions = new ConcurrentHashMap<>();

    public TriggerWorker(EventPublisher eventPublisher, ExecutorService executorService, MessageBroker messageBroker) {
        this.eventPublisher = eventPublisher;
        this.executorService = executorService;
        this.messageBroker = messageBroker;
    }

    @SuppressFBWarnings("NP")
    public void handle(TriggerExecution triggerExecution) {
        logger.debug("Received trigger: {}", triggerExecution);

        CountDownLatch latch = new CountDownLatch(1);

        Future<?> future = executorService.submit(() -> {
            try {
                eventPublisher.publishEvent(new TriggerStartedWorkflowEvent(triggerExecution.getWorkflowExecutionId()));

                TriggerExecution completedTriggerExecution = doExecuteTask(triggerExecution);

                messageBroker.send(TriggerMessageRoute.TRIGGERS_COMPLETIONS, completedTriggerExecution);
            } catch (InterruptedException e) {
                // ignore
            } catch (Exception e) {
                TriggerExecutionFuture<?> triggerExecutionFuture = triggerExecutions.get(
                    triggerExecution.getWorkflowExecutionId());

                if (triggerExecutionFuture == null || !triggerExecutionFuture.isCancelled()) {
                    handleException(triggerExecution, e);
                }
            } finally {
                latch.countDown();
            }
        });

        triggerExecutions.put(triggerExecution.getWorkflowExecutionId(), new TriggerExecutionFuture<>(
            triggerExecution, future));

        try {
            future.get(calculateTimeout(triggerExecution), TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            handleException(triggerExecution, e);
        } catch (CancellationException e) {
            logger.debug("Cancelled trigger: {}", triggerExecution.getWorkflowExecutionId());
        } finally {
            try {
                latch.await();
            } catch (InterruptedException e) {
                handleException(triggerExecution, e);
            }

            triggerExecutions.remove(triggerExecution.getWorkflowExecutionId());
        }
    }

    public void handle(ControlTrigger controlTrigger) {
        logger.debug("Received control trigger: {}", controlTrigger);

        // TODO
    }

    private TriggerExecution doExecuteTask(TriggerExecution triggerExecution) throws Exception {
        // TODO
        System.out.println(triggerExecution);

        return null;
    }

    private long calculateTimeout(TriggerExecution triggerExecution) {
        if (triggerExecution.getTimeout() != null) {
            Duration duration = Duration.parse("PT" + triggerExecution.getTimeout());

            return duration.toMillis();
        }

        return DEFAULT_TIME_OUT;
    }

    private void handleException(TriggerExecution triggerExecution, Exception exception) {
        logger.error(exception.getMessage(), exception);

        triggerExecution.setError(
            new ExecutionError(exception.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(exception))));
        triggerExecution.setStatus(TriggerExecution.Status.FAILED);

        messageBroker.send(SystemMessageRoute.ERRORS, triggerExecution);
    }

    private static class TriggerExecutionFuture<T> implements Future<T> {

        private final Future<T> future;
        private final TriggerExecution triggerExecution;

        TriggerExecutionFuture(TriggerExecution triggerExecution, Future<T> future) {
            this.triggerExecution = Objects.requireNonNull(triggerExecution);
            this.future = Objects.requireNonNull(future);
        }

        @Override
        public boolean cancel(boolean aMayInterruptIfRunning) {
            return future.cancel(aMayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return future.isCancelled();
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return future.get();
        }

        @Override
        public T get(long timeout, TimeUnit timeUnit)
            throws InterruptedException, ExecutionException, TimeoutException {

            return future.get(timeout, timeUnit);
        }
    }
}
