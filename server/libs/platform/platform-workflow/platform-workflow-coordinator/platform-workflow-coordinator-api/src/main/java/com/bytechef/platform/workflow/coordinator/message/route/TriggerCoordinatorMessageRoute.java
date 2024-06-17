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

package com.bytechef.platform.workflow.coordinator.message.route;

import com.bytechef.message.route.MessageRoute;

/**
 * @author Ivica Cardic
 */
public enum TriggerCoordinatorMessageRoute implements MessageRoute {

    APPLICATION_EVENTS(MessageRoute.Exchange.MESSAGE, "trigger.application_events"),
    ERROR_EVENTS(MessageRoute.Exchange.MESSAGE, "trigger.error_events"),
    TRIGGER_EXECUTION_COMPLETE_EVENTS(MessageRoute.Exchange.MESSAGE, "trigger.trigger_execution_complete_events"),
    TRIGGER_LISTENER_EVENTS(MessageRoute.Exchange.MESSAGE, "trigger.trigger_listener_events"),
    TRIGGER_POLL_EVENTS(MessageRoute.Exchange.MESSAGE, "trigger.trigger_poll_events"),
    TRIGGER_WEBHOOK_EVENTS(MessageRoute.Exchange.MESSAGE, "trigger.trigger_webhook_events");

    private MessageRoute.Exchange exchange;
    private String routeName;

    TriggerCoordinatorMessageRoute() {
    }

    TriggerCoordinatorMessageRoute(MessageRoute.Exchange exchange, String routeName) {
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
        return "TriggerCoordinatorMessageRoute{" +
            "exchange=" + exchange +
            ", routeName='" + routeName + '\'' +
            "} ";
    }
}
