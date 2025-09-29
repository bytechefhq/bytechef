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

package com.bytechef.component.delay;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.delay.constant.DelayConstants.DELAY;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.delay.action.DelaySleepAction;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ScheduleComponentDefinition;
import com.bytechef.platform.scheduler.TriggerScheduler;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(DELAY + "_v1_ComponentHandler")
public class DelayComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;

    public DelayComponentHandler(TriggerScheduler triggerScheduler) {
        this.componentDefinition = new DelayComponentDefinitionImpl(triggerScheduler);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class DelayComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements ScheduleComponentDefinition {

        public DelayComponentDefinitionImpl(TriggerScheduler triggerScheduler) {
            super(
                component(DELAY)
                    .title("Delay")
                    .description("Sets a value which can then be referenced in other tasks.")
                    .categories(ComponentCategory.HELPERS)
                    .icon("path:assets/delay.svg")
                    .actions(new DelaySleepAction(triggerScheduler).actionDefinition));
        }
    }
}
