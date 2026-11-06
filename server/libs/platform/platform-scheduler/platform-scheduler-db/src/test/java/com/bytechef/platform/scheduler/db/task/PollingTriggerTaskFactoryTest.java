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

package com.bytechef.platform.scheduler.db.task;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.event.TriggerPollEvent;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import java.time.Instant;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

class PollingTriggerTaskFactoryTest {

    @Test
    void testExecutePublishesTriggerPollEvent() {
        ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 5L, "workflow-uuid", "trigger_1");
        Task<PollingTriggerData> task = PollingTriggerTaskFactory.create(eventPublisher);
        TaskInstance<PollingTriggerData> taskInstance = DbSchedulerTaskDescriptors.POLLING_TRIGGER
            .instance(workflowExecutionId.toString())
            .data(new PollingTriggerData(5))
            .build();

        task.execute(taskInstance, TaskTestSupport.executionContext(taskInstance, Instant.now()));

        ArgumentCaptor<TriggerPollEvent> captor = ArgumentCaptor.forClass(TriggerPollEvent.class);

        Mockito.verify(eventPublisher)
            .publishEvent(captor.capture());
        Assertions.assertThat(captor.getValue()
            .getWorkflowExecutionId()
            .toString())
            .isEqualTo(workflowExecutionId.toString());
    }
}
