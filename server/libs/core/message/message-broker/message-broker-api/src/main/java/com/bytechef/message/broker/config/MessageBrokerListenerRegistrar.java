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

package com.bytechef.message.broker.config;

import com.bytechef.message.route.MessageRoute;

/**
 * @author Ivica Cardic
 */
public interface MessageBrokerListenerRegistrar<T> {

    void registerListenerEndpoint(
        T listenerEndpointRegistrar, MessageRoute messageRoute, int concurrency, Object delegate, String methodName);

    /**
     * Stops all listener endpoints that were registered through this registrar implementation. Implementations should
     * make a best-effort attempt to stop message consumption so that the engine stops processing messages.
     */
    void stopListenerEndpoints();

    /**
     * Starts (or restarts) all listener endpoints that were registered through this registrar implementation.
     * Implementations should make a best-effort attempt to resume message consumption.
     */
    void startListenerEndpoints();
}
