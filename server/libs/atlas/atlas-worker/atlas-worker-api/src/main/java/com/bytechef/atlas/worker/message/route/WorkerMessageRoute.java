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

package com.bytechef.atlas.worker.message.route;

import com.bytechef.message.route.MessageRoute;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class WorkerMessageRoute implements MessageRoute {

    public static final WorkerMessageRoute CONTROL_EVENTS = new WorkerMessageRoute(
        Exchange.CONTROL, "task.control_events");
    public static final WorkerMessageRoute TASK_EXECUTION_EVENTS = new WorkerMessageRoute(
        Exchange.MESSAGE, "task.task_execution_events");

    protected Exchange exchange;
    protected String routeName;

    private WorkerMessageRoute() {
    }

    private WorkerMessageRoute(Exchange exchange, String routeName) {
        this.exchange = exchange;
        this.routeName = routeName;
    }

    public static WorkerMessageRoute ofTaskMessageRoute(String routName) {
        if (Objects.equals(routName, "tasks")) {
            return TASK_EXECUTION_EVENTS;
        } else {
            return new WorkerMessageRoute(Exchange.MESSAGE, routName);
        }
    }

    @Override
    public Exchange getExchange() {
        return exchange;
    }

    @Override
    public String getName() {
        return routeName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof WorkerMessageRoute that)) {
            return false;
        }

        return exchange == that.exchange && Objects.equals(routeName, that.routeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exchange, routeName);
    }

    @Override
    public String toString() {
        return "WorkerMessageRoute{" +
            "exchange=" + exchange +
            ", routeName='" + routeName + '\'' +
            '}';
    }
}
