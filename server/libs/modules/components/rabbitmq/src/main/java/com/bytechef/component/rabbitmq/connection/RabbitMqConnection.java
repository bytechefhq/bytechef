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

package com.bytechef.component.rabbitmq.connection;

import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.rabbitmq.constant.RabbitMqConstants;

/**
 * @author Ivica Cardic
 */
public class RabbitMqConnection {

    public static final ComponentDSL.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .properties(
            string(RabbitMqConstants.HOSTNAME).required(true)
                .defaultValue("localhost"),
            integer(RabbitMqConstants.PORT).required(true)
                .defaultValue(5672),
            string(RabbitMqConstants.USERNAME),
            string(RabbitMqConstants.PASSWORD));
}
