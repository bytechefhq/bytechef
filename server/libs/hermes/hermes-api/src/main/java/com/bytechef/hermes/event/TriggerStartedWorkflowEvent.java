
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

package com.bytechef.hermes.event;

import com.bytechef.event.AbstractWorkflowEvent;

/**
 * @author Ivica Cardic
 */
public class TriggerStartedWorkflowEvent extends AbstractWorkflowEvent {

    public static final String TRIGGER_STARTED = "trigger.started";

    private final long triggerExecutionId;

    public TriggerStartedWorkflowEvent(long triggerExecutionId) {
        super();

        this.triggerExecutionId = triggerExecutionId;
    }

    public long getTriggerExecutionId() {
        return triggerExecutionId;
    }

    @Override
    public String toString() {
        return "TriggerStartedWorkflowEvent{" +
            "triggerExecutionId=" + triggerExecutionId +
            ", createdDate=" + createdDate +
            ", type='" + type + '\'' +
            "} " + super.toString();
    }
}
