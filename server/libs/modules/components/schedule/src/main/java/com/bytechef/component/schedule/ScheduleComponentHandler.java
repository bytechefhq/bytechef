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

package com.bytechef.component.schedule;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.schedule.constant.ScheduleConstants.SCHEDULE;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.schedule.trigger.ScheduleCronTrigger;
import com.bytechef.component.schedule.trigger.ScheduleEveryDayTrigger;
import com.bytechef.component.schedule.trigger.ScheduleEveryMonthTrigger;
import com.bytechef.component.schedule.trigger.ScheduleEveryWeekTrigger;
import com.bytechef.component.schedule.trigger.ScheduleIntervalTrigger;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ScheduleComponentDefinition;
import com.bytechef.platform.scheduler.TriggerScheduler;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(SCHEDULE + "_v1_ComponentHandler")
public class ScheduleComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;

    public ScheduleComponentHandler(TriggerScheduler triggerScheduler) {
        this.componentDefinition = new ScheduleComponentDefinitionImpl(triggerScheduler);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class ScheduleComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements ScheduleComponentDefinition {

        public ScheduleComponentDefinitionImpl(TriggerScheduler triggerScheduler) {
            super(component(SCHEDULE)
                .title("Schedule")
                .description(
                    "With the Scheduled trigger, you can initiate customized workflows at specific time intervals.")
                .icon("path:assets/schedule.svg")
                .triggers(
                    new ScheduleEveryDayTrigger(triggerScheduler).triggerDefinition,
                    new ScheduleEveryWeekTrigger(triggerScheduler).triggerDefinition,
                    new ScheduleEveryMonthTrigger(triggerScheduler).triggerDefinition,
                    new ScheduleIntervalTrigger(triggerScheduler).triggerDefinition,
                    new ScheduleCronTrigger(triggerScheduler).triggerDefinition));
        }
    }
}
