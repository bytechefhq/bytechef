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

package com.bytechef.ee.platform.scheduler.aws.listeners;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.LocalDateTimeUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.workflow.coordinator.event.TriggerListenerEvent;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.util.Date;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;

public class ScheduleListener {
    private ApplicationEventPublisher eventPublisher;

    public ScheduleListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @SqsListener("schedule-queue")
    public void onSchedule(String message) {
        Date fireTime = new Date();
        String[] split = message.split("\\|_\\$plitter_\\|");

        eventPublisher.publishEvent(
            new TriggerListenerEvent(
                new TriggerListenerEvent.ListenerParameters(
                    WorkflowExecutionId.parse(split[1]),
                    LocalDateTimeUtils.toLocalDateTime(fireTime),
                    MapUtils.concat(
                        Map.of("datetime", fireTime.toString()),
                        JsonUtils.readMap(split[0], String.class)))));
    }
}
