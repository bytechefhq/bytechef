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

package com.bytechef.platform.webhook.web.websocket;

import com.bytechef.message.event.MessageEvent;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class CallStatusEvent implements MessageEvent<CallStatusMessageRoute> {

    private Instant createDate;
    private Map<String, Object> metadata = new HashMap<>();
    private Map<String, String> parameters = new HashMap<>();
    private CallStatusMessageRoute route;

    private CallStatusEvent() {
    }

    public CallStatusEvent(Map<String, String> parameters) {
        this.createDate = Instant.now();
        this.parameters = new HashMap<>(parameters);
        this.route = CallStatusMessageRoute.CALL_STATUS_EVENTS;
    }

    @Override
    public Instant getCreateDate() {
        return createDate;
    }

    @Override
    public Object getMetadata(String name) {
        return metadata.get(name);
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public CallStatusMessageRoute getRoute() {
        return route;
    }

    @Override
    public void putMetadata(String name, Object value) {
        metadata.put(name, value);
    }

    @Override
    public String toString() {
        return "CallStatusEvent{" +
            "createDate=" + createDate +
            ", route=" + route +
            "} ";
    }
}
