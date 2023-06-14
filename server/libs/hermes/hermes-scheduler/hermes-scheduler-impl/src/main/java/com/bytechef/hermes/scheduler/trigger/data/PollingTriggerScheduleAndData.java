
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

package com.bytechef.hermes.scheduler.trigger.data;

import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.github.kagkarlsson.scheduler.task.helper.ScheduleAndData;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Ivica Cardic
 */
public class PollingTriggerScheduleAndData implements ScheduleAndData {

    private final WorkflowExecutionId data;
    private final Schedule schedule;

    @SuppressFBWarnings("EI")
    public PollingTriggerScheduleAndData(Schedule schedule, WorkflowExecutionId workflowExecutionId) {
        this.data = workflowExecutionId;
        this.schedule = schedule;
    }

    @Override
    public WorkflowExecutionId getData() {
        return data;
    }

    @Override
    public Schedule getSchedule() {
        return schedule;
    }

    @Override
    public String toString() {
        return "PollScheduleAndData{" +
            "data=" + data +
            ", schedule=" + schedule +
            '}';
    }
}
