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

package com.bytechef.platform.workflow.coordinator.event;

import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.message.route.TriggerCoordinatorMessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;

/**
 * @author Ivica Cardic
 */
public class TriggerListenerEvent extends AbstractEvent {

    private ListenerParameters listenerParameters;

    private TriggerListenerEvent() {
    }

    @SuppressFBWarnings("EI")
    public TriggerListenerEvent(ListenerParameters listenerParameters) {
        super(TriggerCoordinatorMessageRoute.TRIGGER_LISTENER_EVENTS);

        this.listenerParameters = listenerParameters;
    }

    public ListenerParameters getListenerParameters() {
        return listenerParameters;
    }

    public Instant getExecutionDate() {
        return listenerParameters.executionDate;
    }

    public Object getOutput() {
        return listenerParameters.output;
    }

    public WorkflowExecutionId getWorkflowExecutionId() {
        return listenerParameters.workflowExecutionId;
    }

    @Override
    public String toString() {
        return "TriggerListenerEvent{" +
            "listenerParameters=" + listenerParameters +
            ", createdDate=" + createDate +
            ", route=" + route +
            "} ";
    }

    public record ListenerParameters(
        WorkflowExecutionId workflowExecutionId, Instant executionDate, Object output) {
    }
}
