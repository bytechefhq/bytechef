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

/**
 * @author Ivica Cardic
 */
public class TriggerPollEvent extends AbstractEvent {

    private WorkflowExecutionId workflowExecutionId;

    private TriggerPollEvent() {
    }

    @SuppressFBWarnings("EI")
    public TriggerPollEvent(WorkflowExecutionId workflowExecutionId) {
        super(TriggerCoordinatorMessageRoute.TRIGGER_POLL_EVENTS);

        this.workflowExecutionId = workflowExecutionId;
    }

    public WorkflowExecutionId getWorkflowExecutionId() {
        return workflowExecutionId;
    }

    public void setWorkflowExecutionId(WorkflowExecutionId workflowExecutionId) {
        this.workflowExecutionId = workflowExecutionId;
    }

    @Override
    public String toString() {
        return "TriggerPollEvent{" +
            "workflowExecutionId=" + workflowExecutionId +
            ", createdDate=" + createDate +
            ", route=" + route +
            "} ";
    }
}
