
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

package com.bytechef.atlas.coordinator.message.route;

import com.bytechef.message.route.MessageRoute;

/**
 * @author Ivica Cardic
 */
public class CoordinatorMessageRoute implements MessageRoute {

    public static final CoordinatorMessageRoute APPLICATION_EVENTS = new CoordinatorMessageRoute(
        Exchange.MESSAGE, "task.application_events");
    public static final CoordinatorMessageRoute ERROR_EVENTS = new CoordinatorMessageRoute(
        Exchange.MESSAGE, "task.error_events");
    public static final CoordinatorMessageRoute JOB_RESUME_EVENTS =
        new CoordinatorMessageRoute(Exchange.MESSAGE, "task.job_resume_events");
    public static final CoordinatorMessageRoute JOB_START_EVENTS =
        new CoordinatorMessageRoute(Exchange.MESSAGE, "task.job_start_events");
    public static final CoordinatorMessageRoute JOB_STOP_EVENTS =
        new CoordinatorMessageRoute(Exchange.MESSAGE, "task.job_stop_events");
    public static final CoordinatorMessageRoute TASK_EXECUTION_COMPLETE_EVENTS =
        new CoordinatorMessageRoute(Exchange.MESSAGE, "task.task_execution_complete_events");
    protected MessageRoute.Exchange exchange;
    protected String routeName;

    private CoordinatorMessageRoute() {
    }

    private CoordinatorMessageRoute(MessageRoute.Exchange exchange, String routeName) {
        this.exchange = exchange;
        this.routeName = routeName;
    }

    @Override
    public MessageRoute.Exchange getExchange() {
        return exchange;
    }

    @Override
    public String getName() {
        return routeName;
    }

    @Override
    public String toString() {
        return "TaskMessageRoute{" +
            "exchange=" + exchange +
            ", routeName='" + routeName + '\'' +
            '}';
    }
}
