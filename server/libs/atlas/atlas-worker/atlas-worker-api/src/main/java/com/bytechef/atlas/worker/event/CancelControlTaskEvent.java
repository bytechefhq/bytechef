
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

package com.bytechef.atlas.worker.event;

import com.bytechef.atlas.configuration.task.CancelControlTask;
import com.bytechef.atlas.worker.message.route.WorkerMessageRoute;
import com.bytechef.message.event.MessageEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDateTime;

/**
 * @author Ivica Cardic
 */
public class CancelControlTaskEvent implements MessageEvent<WorkerMessageRoute> {

    private LocalDateTime createdDate;
    private CancelControlTask controlTask;

    private CancelControlTaskEvent() {
    }

    public CancelControlTaskEvent(CancelControlTask controlTask) {
        this.createdDate = LocalDateTime.now();
        this.controlTask = controlTask;
    }

    @SuppressFBWarnings("EI")
    public CancelControlTask getControlTask() {
        return controlTask;
    }

    @Override
    public LocalDateTime getCreateDate() {
        return createdDate;
    }

    @Override
    public WorkerMessageRoute getRoute() {
        return WorkerMessageRoute.CONTROL_EVENTS;
    }

    public String getType() {
        return controlTask.getType();
    }

    @Override
    public String toString() {
        return "CancelControlTaskEvent{" +
            "controlTask=" + controlTask +
            "} ";
    }
}
