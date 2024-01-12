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

package com.bytechef.platform.workflow.coordinator.event;

import com.bytechef.message.Prioritizable;
import com.bytechef.platform.workflow.coordinator.message.route.TriggerCoordinatorMessageRoute;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 */
public class TriggerExecutionCompleteEvent extends AbstractEvent implements Prioritizable {

    private TriggerExecution triggerExecution;

    private TriggerExecutionCompleteEvent() {
    }

    @SuppressFBWarnings("EI")
    public TriggerExecutionCompleteEvent(TriggerExecution triggerExecution) {
        super(TriggerCoordinatorMessageRoute.TRIGGER_EXECUTION_COMPLETE_EVENTS);

        Validate.notNull(triggerExecution, "'triggerExecution' must not be null");

        this.triggerExecution = triggerExecution;
    }

    @SuppressFBWarnings("EI")
    public TriggerExecution getTriggerExecution() {
        return triggerExecution;
    }

    @Override
    public int getPriority() {
        return triggerExecution.getPriority();
    }

    @Override
    public String toString() {
        return "TriggerCompleteEvent{" +
            "triggerExecution=" + triggerExecution +
            ", createdDate=" + createDate +
            ", route=" + route +
            "} ";
    }
}
