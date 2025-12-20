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

package com.bytechef.platform.workflow.worker;

import com.bytechef.error.ExecutionError;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.configuration.domain.CancelControlTrigger;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.event.TriggerExecutionCompleteEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerExecutionErrorEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerStartedApplicationEvent;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.worker.event.CancelControlTriggerEvent;
import com.bytechef.platform.workflow.worker.event.TriggerExecutionEvent;
import com.bytechef.platform.workflow.worker.exception.TriggerExecutionException;
import com.bytechef.platform.workflow.worker.executor.TriggerWorkerExecutor;
import com.bytechef.platform.workflow.worker.trigger.handler.TriggerHandler;
import com.bytechef.platform.workflow.worker.trigger.handler.TriggerHandlerResolver;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
public class TriggerWorker {

    private static final Logger logger = LoggerFactory.getLogger(TriggerWorker.class);

    private static final long DEFAULT_TIME_OUT = 24 * 60 * 60 * 1000; // 24 hours

    private final ApplicationEventPublisher eventPublisher;
    private final Map<WorkflowExecutionId, TriggerExecutionFuture<?>> triggerExecutions = new ConcurrentHashMap<>();
    private final TriggerFileStorage triggerFileStorage;
    private final TriggerHandlerResolver triggerHandlerResolver;
    private final TriggerWorkerExecutor triggerWorkerExecutor;

    public TriggerWorker(
        ApplicationEventPublisher eventPublisher, TriggerFileStorage triggerFileStorage,
        TriggerHandlerResolver triggerHandlerResolver, TriggerWorkerExecutor triggerWorkerExecutor) {

        this.eventPublisher = eventPublisher;
        this.triggerFileStorage = triggerFileStorage;
        this.triggerHandlerResolver = triggerHandlerResolver;
        this.triggerWorkerExecutor = triggerWorkerExecutor;
    }

    public void onTriggerExecutionEvent(TriggerExecutionEvent triggerExecutionEvent) {
        if (logger.isTraceEnabled()) {
            logger.trace("onTriggerExecutionEvent: triggerExecutionEvent={}", triggerExecutionEvent);
        }

        TriggerExecution triggerExecution = triggerExecutionEvent.getTriggerExecution();
        CountDownLatch latch = new CountDownLatch(1);

        Future<?> future = triggerWorkerExecutor.submit(() -> {
            try {
                eventPublisher.publishEvent(
                    new TriggerStartedApplicationEvent(Validate.notNull(triggerExecution.getId(), "id")));

                TriggerExecution execution = doExecuteTrigger(triggerExecution);

                TriggerExecution.Status status = execution.getStatus();

                if (status.equals(TriggerExecution.Status.COMPLETED)) {
                    eventPublisher.publishEvent(new TriggerExecutionCompleteEvent(execution));
                }
            } catch (InterruptedException e) {
                if (logger.isTraceEnabled()) {
                    logger.trace(e.getMessage(), e);
                }
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

        triggerExecutions.put(
            triggerExecution.getWorkflowExecutionId(), new TriggerExecutionFuture<>(future, triggerExecution));

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

    public void onCancelControlTriggerEvent(MessageEvent<?> event) {
        if (event instanceof CancelControlTriggerEvent cancelControlTriggerEvent) {
            CancelControlTrigger cancelControlTrigger = cancelControlTriggerEvent.getControlTrigger();

            logger.debug("onCancelControlTriggerEvent: cancelControlTrigger={}", cancelControlTrigger);

            long id = cancelControlTrigger.getTriggerExecutionId();

            for (TriggerExecutionFuture<?> triggerExecutionFuture : triggerExecutions.values()) {
                if (Objects.equals(triggerExecutionFuture.triggerExecution.getId(), id)) {
                    logger.info("Cancelling trigger id={}", triggerExecutionFuture.triggerExecution.getId());

                    triggerExecutionFuture.cancel(true);
                }
            }
        }
    }

    private TriggerExecution doExecuteTrigger(TriggerExecution triggerExecution) throws Exception {
        Instant startDate = Instant.now();

        long startTime = startDate.toEpochMilli();

        triggerExecution.setStartDate(startDate);

        TriggerHandler triggerHandler = triggerHandlerResolver.resolve(triggerExecution);

        TriggerOutput triggerOutput;

        try {
            triggerOutput = triggerHandler.handle(triggerExecution.clone());
        } catch (TriggerExecutionException exception) {
            handleException(triggerExecution, exception);

            return triggerExecution;
        }

        if (triggerOutput == null) {
            triggerExecution.setState(null);
        } else {
            triggerExecution.setBatch(triggerOutput.batch());

            if (triggerOutput.value() != null) {
                triggerExecution.setOutput(
                    triggerFileStorage.storeTriggerExecutionOutput(
                        Validate.notNull(triggerExecution.getId(), "id"), triggerOutput.value()));
            }

            if (triggerOutput.state() == null) {
                triggerExecution.setState(null);
            } else {
                triggerExecution.setState(triggerOutput.state());
            }
        }

        triggerExecution.setEndDate(Instant.now());
        triggerExecution.setExecutionTime(System.currentTimeMillis() - startTime);
        triggerExecution.setStatus(TriggerExecution.Status.COMPLETED);

        return triggerExecution;
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

        Instant endDate = Instant.now();
        Instant startDate = triggerExecution.getStartDate();

        triggerExecution.setEndDate(endDate);
        triggerExecution.setError(
            new ExecutionError(exception.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(exception))));
        triggerExecution.setExecutionTime(endDate.toEpochMilli() - startDate.toEpochMilli());
        triggerExecution.setStatus(TriggerExecution.Status.FAILED);

        eventPublisher.publishEvent(new TriggerExecutionErrorEvent(triggerExecution));
    }

    private record TriggerExecutionFuture<T>(Future<T> future, TriggerExecution triggerExecution) implements Future<T> {

        private TriggerExecutionFuture(Future<T> future, TriggerExecution triggerExecution) {
            this.triggerExecution = Validate.notNull(triggerExecution, "triggerExecution");
            this.future = Validate.notNull(future, "future");
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
