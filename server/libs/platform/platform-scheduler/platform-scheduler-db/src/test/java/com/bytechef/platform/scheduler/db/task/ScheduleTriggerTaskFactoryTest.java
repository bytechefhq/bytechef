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
import com.bytechef.platform.workflow.coordinator.event.TriggerListenerEvent;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.TaskInstance;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(ObjectMapperSetupExtension.class)
class ScheduleTriggerTaskFactoryTest {

    private final ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);

    @Test
    void testExecutePublishesTriggerListenerEventWithFireTimeAndLocalDateTime() {
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 123L, "workflow-uuid", "trigger_1");
        Instant fireTime = Instant.parse("2030-06-01T07:00:00Z");
        Task<ScheduleTriggerData> task = ScheduleTriggerTaskFactory.create(eventPublisher);
        TaskInstance<ScheduleTriggerData> taskInstance = DbSchedulerTaskDescriptors.SCHEDULE_TRIGGER
            .instance(workflowExecutionId.toString())
            .data(new ScheduleTriggerData(
                "0 0 9 * * ?", "Europe/Zagreb", "{\"expression\":\"0 9 * * *\",\"timezone\":\"Europe/Zagreb\"}"))
            .build();

        task.execute(taskInstance, TaskTestSupport.executionContext(taskInstance, fireTime));

        ArgumentCaptor<TriggerListenerEvent> captor = ArgumentCaptor.forClass(TriggerListenerEvent.class);

        Mockito.verify(eventPublisher)
            .publishEvent(captor.capture());

        TriggerListenerEvent.ListenerParameters parameters = captor.getValue()
            .getListenerParameters();

        Assertions.assertThat(parameters.workflowExecutionId()
            .toString())
            .isEqualTo(workflowExecutionId.toString());
        Assertions.assertThat(parameters.executionDate())
            .isEqualTo(fireTime);

        @SuppressWarnings("unchecked")
        Map<String, Object> output = (Map<String, Object>) parameters.output();

        Assertions.assertThat(output)
            .containsEntry("fireTime", Date.from(fireTime))
            .containsEntry("dateTime", LocalDateTime.of(2030, 6, 1, 9, 0))
            .containsEntry("expression", "0 9 * * *")
            .containsEntry("timezone", "Europe/Zagreb");
    }

    @Test
    void testExecuteFallsBackToSystemZoneWhenTimezoneMissing() {
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 123L, "workflow-uuid", "trigger_1");
        Instant fireTime = Instant.parse("2030-06-01T07:00:00Z");
        Task<ScheduleTriggerData> task = ScheduleTriggerTaskFactory.create(eventPublisher);
        TaskInstance<ScheduleTriggerData> taskInstance = DbSchedulerTaskDescriptors.SCHEDULE_TRIGGER
            .instance(workflowExecutionId.toString())
            .data(new ScheduleTriggerData("0 0 9 * * ?", "UTC", "{}"))
            .build();

        task.execute(taskInstance, TaskTestSupport.executionContext(taskInstance, fireTime));

        ArgumentCaptor<TriggerListenerEvent> captor = ArgumentCaptor.forClass(TriggerListenerEvent.class);

        Mockito.verify(eventPublisher)
            .publishEvent(captor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> output = (Map<String, Object>) captor.getValue()
            .getListenerParameters()
            .output();

        Assertions.assertThat(output)
            .containsEntry("dateTime", LocalDateTime.ofInstant(fireTime, ZoneId.systemDefault()));
    }
}
