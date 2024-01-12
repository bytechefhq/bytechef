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

import com.bytechef.error.ExecutionError;
import com.bytechef.platform.workflow.coordinator.message.route.TriggerCoordinatorMessageRoute;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 */
public class TriggerExecutionErrorEvent extends AbstractEvent implements ErrorEvent {

    private TriggerExecution triggerExecution;

    private TriggerExecutionErrorEvent() {
    }

    @SuppressFBWarnings("EI")
    public TriggerExecutionErrorEvent(TriggerExecution triggerExecution) {
        super(TriggerCoordinatorMessageRoute.ERROR_EVENTS);

        Validate.notNull(triggerExecution, "'triggerExecution' must not be null");

        this.triggerExecution = triggerExecution;
    }

    @Override
    public ExecutionError getError() {
        return triggerExecution.getError();
    }

    @SuppressFBWarnings("EI")
    public TriggerExecution getTriggerExecution() {
        return triggerExecution;
    }

    @Override
    public String toString() {
        return "TriggerExecutionErrorEvent{" +
            "triggerExecution=" + triggerExecution +
            ", createdDate=" + createDate +
            ", route=" + route +
            "} ";
    }
}
