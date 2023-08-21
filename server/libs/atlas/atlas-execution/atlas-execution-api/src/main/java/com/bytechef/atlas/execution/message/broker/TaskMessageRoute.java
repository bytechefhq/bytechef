
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

package com.bytechef.atlas.execution.message.broker;

import com.bytechef.message.broker.MessageRoute;

/**
 * @author Ivica Cardic
 */
public enum TaskMessageRoute implements MessageRoute {

    JOBS_START(Exchange.MESSAGE, "jobs.start"),
    JOBS_CREATE(Exchange.MESSAGE, "jobs.create"),
    JOBS_RESUME(Exchange.MESSAGE, "jobs.resume"),
    JOBS_STOP(Exchange.MESSAGE, "jobs.stop"),
    TASKS(Exchange.MESSAGE, "tasks"),
    TASKS_COMPLETE(Exchange.MESSAGE, "tasks.complete");

    private final Exchange exchange;
    private final String routeName;

    TaskMessageRoute(Exchange exchange, String routeName) {
        this.exchange = exchange;
        this.routeName = routeName;
    }

    public static MessageRoute ofWorkerRoute(String routName) {
        return switch (routName) {
            case "tasks" -> TASKS;
            default -> new CustomTaskMessageRoute(routName);
        };
    }

    @Override
    public Exchange getExchange() {
        return exchange;
    }

    @Override
    public String toString() {
        return routeName;
    }

    private record CustomTaskMessageRoute(String routName) implements MessageRoute {

        @Override
        public Exchange getExchange() {
            return Exchange.MESSAGE;
        }

        @Override
        public String toString() {
            return routName;
        }
    }
}
