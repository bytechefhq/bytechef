
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

package com.bytechef.hermes.message.broker;

import com.bytechef.message.broker.MessageRoute;

/**
 * @author Ivica Cardic
 */
public enum TriggerMessageRoute implements MessageRoute {

    TRIGGERS(Exchange.MESSAGE, "triggers"),
    TRIGGERS_COMPLETIONS(Exchange.MESSAGE, "triggers.completions");

    private final Exchange exchange;
    private final String routeName;

    TriggerMessageRoute(Exchange exchange, String routeName) {
        this.exchange = exchange;
        this.routeName = routeName;
    }

    public static MessageRoute ofRoute(String routName) {
        return switch (routName) {
            case "triggers" -> TRIGGERS;
            case "triggers.completions" -> TRIGGERS_COMPLETIONS;
            default -> throw new IllegalArgumentException("Route name '%s' does not exist.".formatted(routName));
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
}
