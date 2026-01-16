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

package com.bytechef.config.actuate.endpoint;

import com.bytechef.message.broker.config.MessageBrokerListenerRegistrar;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

/**
 * Custom actuator endpoint that can start/stop all message broker listeners registered via
 * {@link MessageBrokerListenerRegistrar}, effectively controlling whether the engine processes new messages.
 *
 * Endpoint id: messageBrokerListeners Operations: - POST /actuator/messageBrokerListeners/stop - POST
 * /actuator/messageBrokerListeners/start
 *
 * @author Ivica Cardic
 */
@Component
@Endpoint(id = "messageBrokerListeners")
public class MessageBrokerListenerEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(MessageBrokerListenerEndpoint.class);

    private final List<MessageBrokerListenerRegistrar<?>> registrars;

    @SuppressFBWarnings("EI")
    public MessageBrokerListenerEndpoint(List<MessageBrokerListenerRegistrar<?>> registrars) {
        this.registrars = registrars;
    }

    @WriteOperation
    public Map<String, Object> operation(@Selector String action) {
        Map<String, Object> result = new HashMap<>();

        int total = 0;
        switch (action) {
            case "stop":
                for (MessageBrokerListenerRegistrar<?> registrar : registrars) {
                    try {
                        registrar.stopListenerEndpoints();
                        total++;
                    } catch (Exception e) {
                        logger.warn("Failed to stop listeners for registrar {}: {}",
                            registrar.getClass()
                                .getName(),
                            e.getMessage());
                    }
                }
                break;
            case "start":
                for (MessageBrokerListenerRegistrar<?> registrar : registrars) {
                    try {
                        registrar.startListenerEndpoints();
                        total++;
                    } catch (Exception e) {
                        logger.warn("Failed to start listeners for registrar {}: {}",
                            registrar.getClass()
                                .getName(),
                            e.getMessage());
                    }
                }
                break;
            default:
                result.put("status", "ERROR");
                result.put("message", "Unknown action: " + action + ". Supported: stop, start");
                return result;
        }

        result.put("status", "OK");
        result.put("action", action);
        result.put("registrarsProcessed", total);
        return result;
    }
}
