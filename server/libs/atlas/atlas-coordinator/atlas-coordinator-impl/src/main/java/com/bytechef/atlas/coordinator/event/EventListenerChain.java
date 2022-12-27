
/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.coordinator.event;

import com.bytechef.atlas.event.WorkflowEvent;
import java.util.List;

/**
 * @author Arik Cohen
 * @since Jul 5, 2017
 */
public class EventListenerChain implements EventListener {

    private final List<EventListener> eventListeners;

    public EventListenerChain(List<EventListener> eventListeners) {
        this.eventListeners = eventListeners;
    }

    @Override
    public void onApplicationEvent(WorkflowEvent workflowEvent) {
        for (EventListener eventListener : eventListeners) {
            eventListener.onApplicationEvent(workflowEvent);
        }
    }
}
