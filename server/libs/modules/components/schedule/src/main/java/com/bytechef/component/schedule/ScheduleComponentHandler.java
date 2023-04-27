
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

package com.bytechef.component.schedule;

import com.bytechef.component.schedule.trigger.ScheduleCronTrigger;
import com.bytechef.component.schedule.trigger.ScheduleEveryDayTrigger;
import com.bytechef.component.schedule.trigger.ScheduleEveryMonthTrigger;
import com.bytechef.component.schedule.trigger.ScheduleEveryWeekTrigger;
import com.bytechef.component.schedule.trigger.ScheduleIntervalTrigger;
import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.github.kagkarlsson.scheduler.SchedulerClient;
import org.springframework.stereotype.Component;

import static com.bytechef.hermes.component.definition.ComponentDSL.component;

/**
 * @author Ivica Cardic
 */
@Component
public class ScheduleComponentHandler implements ComponentDefinitionFactory {

    private final ComponentDefinition componentDefinition;

    public ScheduleComponentHandler(SchedulerClient schedulerClient) {
        this.componentDefinition = component("schedule")
            .title("Schedule")
            .description(
                "With the Scheduled Trigger, you can initiate customized workflows at specific time intervals.")
            .icon("path:assets/schedule.svg")
            .triggers(
                new ScheduleEveryDayTrigger(schedulerClient).triggerDefinition,
                new ScheduleEveryWeekTrigger(schedulerClient).triggerDefinition,
                new ScheduleEveryMonthTrigger(schedulerClient).triggerDefinition,
                new ScheduleIntervalTrigger(schedulerClient).triggerDefinition,
                new ScheduleCronTrigger(schedulerClient).triggerDefinition);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
