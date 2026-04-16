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

package com.bytechef.message.route;

/**
 * @author Ivica Cardic
 */
public interface MessageRoute {

    enum Exchange {

        CONTROL("exchange.control"),
        MESSAGE("exchange.message");

        private final String name;

        Exchange(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    default boolean isControlExchange() {
        return Exchange.CONTROL == getExchange();
    }

    default boolean isMessageExchange() {
        return Exchange.MESSAGE == getExchange();
    }

    /**
     * Whether messages on this route must be delivered in strict FIFO order to receivers. In-memory broker
     * implementations may dispatch unordered routes on a shared thread pool for throughput; ordered routes require
     * per-route serial dispatch so tokens (e.g., SSE stream events) reach the receiver in the sequence they were sent.
     */
    default boolean isOrdered() {
        return false;
    }

    Exchange getExchange();

    String getName();
}
