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

package com.bytechef.component.delay.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.delay.constant.DelayConstants.MILLIS;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class DelaySleepAction {

    public final ModifiableActionDefinition actionDefinition = action("sleep")
        .title("Sleep")
        .description("Delay action execution.")
        .properties(
            integer(MILLIS)
                .label("Millis")
                .description("Time in milliseconds.")
                .required(true)
                .defaultValue(1))
        .perform(this::perform);

    private final TriggerScheduler triggerScheduler;

    public DelaySleepAction(TriggerScheduler triggerScheduler) {
        this.triggerScheduler = triggerScheduler;
    }

    protected Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context)
        throws InterruptedException {

        long millis;
        if (inputParameters.containsKey(MILLIS)) {
            millis = inputParameters.getLong(MILLIS);
        } else if (inputParameters.containsKey("duration")) {
            Duration duration = inputParameters.getDuration("duration");
            millis = duration.toMillis();
        } else {
            millis = 1000;
        }

        LocalDateTime executeAt = scheduleDelay(millis, (ActionContextAware) context);

        return Map.of("scheduledAt", executeAt, "delayMillis", millis);
    }

    private LocalDateTime scheduleDelay(long millis, ActionContextAware context) throws InterruptedException {
        String taskExecutionId = String.valueOf(context.getJobId());
        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.parse(context.getWorkflowId());

        LocalDateTime executeAt = LocalDateTime.now()
            .plusNanos(millis * 1_000_000);

        triggerScheduler.scheduleOneTimeTask(
            executeAt, Map.of("delayMillis", millis), workflowExecutionId, taskExecutionId);

        return executeAt;
    }
}
