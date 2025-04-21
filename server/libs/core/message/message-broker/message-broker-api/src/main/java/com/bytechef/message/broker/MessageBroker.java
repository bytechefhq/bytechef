/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.message.broker;

import com.bytechef.message.route.MessageRoute;

/**
 * Abstraction for sending messages between the various components of the application. Implementations are responsible
 * for the guaranteed delivery of the message.
 *
 * @author Arik Cohen
 * @since Jun 18, 2016
 */
public interface MessageBroker {

    /**
     * @param route   A representation used for routing the message to the appropriate destination.
     * @param message The message to send.
     */
    void send(MessageRoute route, Object message);
}
