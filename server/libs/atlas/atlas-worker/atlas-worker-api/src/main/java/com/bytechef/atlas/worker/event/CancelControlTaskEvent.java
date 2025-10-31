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

package com.bytechef.atlas.worker.event;

import com.bytechef.atlas.configuration.domain.CancelControlTask;
import com.bytechef.atlas.worker.message.route.TaskWorkerMessageRoute;
import com.bytechef.message.event.MessageEvent;

/**
 * @author Ivica Cardic
 */
public class CancelControlTaskEvent extends AbstractEvent implements MessageEvent<TaskWorkerMessageRoute> {

    private CancelControlTask controlTask;

    private CancelControlTaskEvent() {
    }

    public CancelControlTaskEvent(CancelControlTask controlTask) {
        this.controlTask = controlTask;
    }

    public CancelControlTask getControlTask() {
        return controlTask;
    }

    @Override
    public TaskWorkerMessageRoute getRoute() {
        return TaskWorkerMessageRoute.CONTROL_EVENTS;
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
