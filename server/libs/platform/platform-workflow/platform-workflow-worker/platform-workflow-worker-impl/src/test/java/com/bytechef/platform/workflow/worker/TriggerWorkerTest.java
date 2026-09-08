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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.event.TriggerExecutionErrorEvent;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.worker.event.TriggerExecutionEvent;
import com.bytechef.platform.workflow.worker.executor.TriggerWorkerExecutor;
import com.bytechef.platform.workflow.worker.trigger.handler.TriggerHandler;
import com.bytechef.platform.workflow.worker.trigger.handler.TriggerHandlerResolver;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class TriggerWorkerTest {

    @Test
    @Timeout(15)
    void testTimedOutTriggerIsCancelledAndInterrupted() throws Exception {
        CountDownLatch interruptedLatch = new CountDownLatch(1);

        TriggerHandler blockingTriggerHandler = triggerExecution -> {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException interruptedException) {
                interruptedLatch.countDown();

                throw new IllegalStateException(interruptedException);
            }

            return null;
        };

        List<Object> publishedEvents = new CopyOnWriteArrayList<>();

        TriggerWorker triggerWorker = new TriggerWorker(
            publishedEvents::add, Mockito.mock(TriggerFileStorage.class),
            new TriggerHandlerResolver(type -> blockingTriggerHandler), new ThreadPerTaskTriggerWorkerExecutor());

        TriggerExecution triggerExecution = TriggerExecution.builder()
            .id(1L)
            .workflowExecutionId(WorkflowExecutionId.of(PlatformType.AUTOMATION, 1L, "workflow-uuid", "trigger"))
            .workflowTrigger(
                new WorkflowTrigger(Map.of("name", "trigger", "type", "component/v1/trigger", "timeout", "1S")))
            .build();

        triggerWorker.onTriggerExecutionEvent(new TriggerExecutionEvent(triggerExecution));

        assertTrue(interruptedLatch.await(5, TimeUnit.SECONDS), "handler thread was not interrupted");
        assertEquals(TriggerExecution.Status.FAILED, triggerExecution.getStatus());
        assertTrue(publishedEvents.stream()
            .anyMatch(TriggerExecutionErrorEvent.class::isInstance));
    }

    private static final class ThreadPerTaskTriggerWorkerExecutor extends TriggerWorkerExecutor {

        private final ExecutorService executorService = Executors.newCachedThreadPool();

        @Override
        public Future<?> submit(Runnable runnable) {
            return executorService.submit(runnable);
        }
    }
}
