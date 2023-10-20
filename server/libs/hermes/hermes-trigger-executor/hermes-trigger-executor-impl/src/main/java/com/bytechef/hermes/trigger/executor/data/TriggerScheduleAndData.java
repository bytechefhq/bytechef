
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

package com.bytechef.hermes.trigger.executor.data;

import com.bytechef.hermes.trigger.WorkflowTrigger;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import com.github.kagkarlsson.scheduler.task.helper.ScheduleAndData;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class TriggerScheduleAndData implements ScheduleAndData {

    private final Data data;
    private final Schedule schedule;

    @SuppressFBWarnings("EI")
    public TriggerScheduleAndData(
        WorkflowExecutionId workflowExecutionId, WorkflowTrigger workflowTrigger) {

        this(null, workflowExecutionId, workflowTrigger, null);
    }

    @SuppressFBWarnings("EI")
    public TriggerScheduleAndData(
        Schedule schedule, WorkflowExecutionId workflowExecutionId, WorkflowTrigger workflowTrigger,
        Map<String, Object> context) {

        this.data = new Data(context, workflowExecutionId, workflowTrigger);
        this.schedule = schedule;
    }

    @Override
    public Data getData() {
        return data;
    }

    @Override
    public Schedule getSchedule() {
        return schedule;
    }

    @Override
    public String toString() {
        return "TriggerScheduleAndData{" +
            "data=" + data +
            ", schedule=" + schedule +
            '}';
    }

    @SuppressFBWarnings("EI")
    public record Data(
        Map<String, Object> context, WorkflowExecutionId workflowExecutionId, WorkflowTrigger workflowTrigger)
        implements Serializable {
    }
}
