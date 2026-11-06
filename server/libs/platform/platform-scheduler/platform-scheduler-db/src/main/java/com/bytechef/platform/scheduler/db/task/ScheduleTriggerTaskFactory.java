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

import static com.bytechef.platform.scheduler.db.task.DbSchedulerTaskDescriptors.SCHEDULE_TRIGGER;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.event.TriggerListenerEvent;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
public final class ScheduleTriggerTaskFactory {

    private static final Logger log = LoggerFactory.getLogger(ScheduleTriggerTaskFactory.class);

    private ScheduleTriggerTaskFactory() {
    }

    public static Task<ScheduleTriggerData> create(ApplicationEventPublisher eventPublisher) {
        return Tasks.recurringWithPersistentSchedule(SCHEDULE_TRIGGER)
            .execute((taskInstance, executionContext) -> {
                ScheduleTriggerData data = taskInstance.getData();
                WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.parse(taskInstance.getId());
                Instant fireTime = executionContext.getExecution().executionTime;
                Map<String, Object> output = JsonUtils.readMap(data.output(), Object.class);
                Date fireDate = Date.from(fireTime);

                ContextBinding.run(
                    workflowExecutionId.getTenantId(),
                    () -> eventPublisher.publishEvent(
                        new TriggerListenerEvent(
                            new TriggerListenerEvent.ListenerParameters(
                                workflowExecutionId, fireTime,
                                MapUtils.concat(
                                    Map.of("fireTime", fireDate, "dateTime", getFireLocalDateTime(output, fireDate)),
                                    output)))));
            });
    }

    private static LocalDateTime getFireLocalDateTime(Map<String, ?> map, Date fireTime) {
        ZoneId zoneId = ZoneId.systemDefault();

        if (map.containsKey("timezone")) {
            Object value = map.get("timezone");

            if (value instanceof String stringValue) {
                zoneId = ZoneId.of(stringValue);
            }
        }

        if (Objects.equals(zoneId, ZoneId.systemDefault())) {
            log.info("Default ZoneId is used. Workflow definition parameters miss zone id - check/update db values");
        }

        return fireTime.toInstant()
            .atZone(zoneId)
            .toLocalDateTime();
    }
}
