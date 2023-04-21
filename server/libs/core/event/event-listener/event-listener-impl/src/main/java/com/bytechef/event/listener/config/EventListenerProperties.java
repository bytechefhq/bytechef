
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

package com.bytechef.event.listener.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Ivica Cardic
 */
@ConfigurationProperties(prefix = "bytechef.event-listener")
@SuppressFBWarnings("EI")
public class EventListenerProperties {

    private EventListenerSubscriptions subscriptions = new EventListenerSubscriptions();

    public EventListenerSubscriptions getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(EventListenerSubscriptions subscriptions) {
        this.subscriptions = subscriptions;
    }

    public static class EventListenerSubscriptions {

        private int events = 1;

        public int getEvents() {
            return events;
        }

        public void setEvents(int events) {
            this.events = events;
        }
    }
}
