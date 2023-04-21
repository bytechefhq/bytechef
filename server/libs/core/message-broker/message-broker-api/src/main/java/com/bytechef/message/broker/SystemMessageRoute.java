
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

package com.bytechef.message.broker;

/**
 * @author Ivica Cardic
 */
public enum SystemMessageRoute implements MessageRoute {

    CONTROL(Exchange.CONTROL, "system.control"),
    DLQ(Exchange.MESSAGE, "system.dlq"),
    ERRORS(Exchange.MESSAGE, "system.errors"),
    EVENTS(Exchange.MESSAGE, "system.events");

    private final Exchange exchange;
    private final String routeName;

    SystemMessageRoute(Exchange exchange, String routeName) {
        this.exchange = exchange;
        this.routeName = routeName;
    }

    @Override
    public Exchange getExchange() {
        return exchange;
    }

    @Override
    public String toString() {
        return routeName;
    }
}
