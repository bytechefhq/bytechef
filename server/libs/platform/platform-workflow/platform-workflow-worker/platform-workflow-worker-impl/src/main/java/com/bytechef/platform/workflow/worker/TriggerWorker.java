/*
 * Copyright 2023-present ByteChef Inc.
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

import com.bytechef.commons.util.ExceptionUtils;
import com.bytechef.error.ExecutionError;
import com.bytechef.message.event.MessageEvent;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.configuration.domain.CancelControlTrigger;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.coordinator.event.TriggerExecutionCompleteEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerExecutionErrorEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerStartedApplicationEvent;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.worker.executor.TriggerWorkerExecutor;
import com.bytechef.platform.workflow.worker.trigger.event.CancelControlTriggerEvent;
import com.bytechef.platform.workflow.worker.trigger.event.TriggerExecutionEvent;
import com.bytechef.platform.workflow.worker.trigger.handler.TriggerHandler;
import com.bytechef.platform.workflow.worker.trigger.handler.TriggerHandlerResolver;
import java.time.Duration;
import java.time.LocalDateTime;
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
    private final TriggerFileStorage triggerFileStorage;
    private final TriggerWorkerExecutor triggerWorkerExecutor;
    private final Map<WorkflowExecutionId, TriggerExecutionFuture<?>> triggerExecutions = new ConcurrentHashMap<>();
    private final TriggerHandlerResolver triggerHandlerResolver;

    public TriggerWorker(
        ApplicationEventPublisher eventPublisher, TriggerFileStorage triggerFileStorage,
        TriggerWorkerExecutor executorService, TriggerHandlerResolver triggerHandlerResolver) {

        this.eventPublisher = eventPublisher;
        this.triggerFileStorage = triggerFileStorage;
        this.triggerWorkerExecutor = executorService;
        this.triggerHandlerResolver = triggerHandlerResolver;
    }

    public void onTriggerExecutionEvent(TriggerExecutionEvent triggerExecutionEvent) {
        logger.debug("onTriggerExecutionEvent: triggerExecutionEvent={}", triggerExecutionEvent);

        TriggerExecution triggerExecution = triggerExecutionEvent.getTriggerExecution();
        CountDownLatch latch = new CountDownLatch(1);

        Future<?> future = triggerWorkerExecutor.submit(() -> {
            try {
                eventPublisher.publishEvent(
                    new TriggerStartedApplicationEvent(Validate.notNull(triggerExecution.getId(), "id")));

                TriggerExecution completedTriggerExecution = doExecuteTrigger(triggerExecution);

                eventPublisher.publishEvent(new TriggerExecutionCompleteEvent(completedTriggerExecution));
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

        triggerExecutions.put(
            triggerExecution.getWorkflowExecutionId(), new TriggerExecutionFuture<>(triggerExecution, future));

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
        long startTime = System.currentTimeMillis();

        TriggerHandler triggerHandler = triggerHandlerResolver.resolve(triggerExecution);

        TriggerOutput triggerOutput = triggerHandler.handle(triggerExecution.clone());

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

        triggerExecution.setEndDate(LocalDateTime.now());
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

        triggerExecution.setError(
            new ExecutionError(exception.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(exception))));
        triggerExecution.setStatus(TriggerExecution.Status.FAILED);

        eventPublisher.publishEvent(new TriggerExecutionErrorEvent(triggerExecution));
    }

    private record TriggerExecutionFuture<T>(TriggerExecution triggerExecution, Future<T> future) implements Future<T> {

        private TriggerExecutionFuture(TriggerExecution triggerExecution, Future<T> future) {
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
