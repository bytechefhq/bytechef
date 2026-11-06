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

import static com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors.POLLING_TRIGGER;

import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.event.TriggerPollEvent;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
public final class PollingTriggerTaskFactory {

    private PollingTriggerTaskFactory() {
    }

    public static Task<PollingTriggerData> create(ApplicationEventPublisher eventPublisher) {
        return Tasks.recurringWithPersistentSchedule(POLLING_TRIGGER)
            .execute((taskInstance, executionContext) -> {
                WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.parse(taskInstance.getId());

                ContextBinding.run(
                    workflowExecutionId.getTenantId(),
                    () -> eventPublisher.publishEvent(new TriggerPollEvent(workflowExecutionId)));
            });
    }
}
