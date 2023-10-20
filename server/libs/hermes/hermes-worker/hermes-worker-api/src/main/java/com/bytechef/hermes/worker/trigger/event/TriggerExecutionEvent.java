
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

package com.bytechef.hermes.worker.trigger.event;

import com.bytechef.hermes.execution.domain.TriggerExecution;
import com.bytechef.hermes.worker.trigger.message.route.WorkerMessageRoute;
import com.bytechef.message.Prioritizable;
import com.bytechef.message.event.MessageEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.Validate;

import java.time.LocalDateTime;

/**
 * @author Ivica Cardic
 */
public class TriggerExecutionEvent implements Prioritizable, MessageEvent<WorkerMessageRoute> {

    private LocalDateTime createdDate;
    private TriggerExecution triggerExecution;

    private TriggerExecutionEvent() {
    }

    @SuppressFBWarnings("EI")
    public TriggerExecutionEvent(TriggerExecution triggerExecution) {
        super();

        Validate.notNull(triggerExecution, "'triggerExecution' must not be null");

        this.createdDate = LocalDateTime.now();
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
    public LocalDateTime getCreateDate() {
        return createdDate;
    }

    public WorkerMessageRoute getRoute() {
        return WorkerMessageRoute.TRIGGER_EXECUTION_EVENTS;
    }

    @Override
    public String toString() {
        return "TriggerExecutionEvent{" +
            "triggerExecution=" + triggerExecution +
            ", createdDate=" + createdDate +
            "} ";
    }
}
