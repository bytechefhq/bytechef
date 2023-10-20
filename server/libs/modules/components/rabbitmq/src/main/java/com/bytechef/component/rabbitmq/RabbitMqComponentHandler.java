
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

package com.bytechef.component.rabbitmq;

import com.bytechef.component.rabbitmq.action.RabbitMqSendMessageAction;
import com.bytechef.component.rabbitmq.connection.RabbitMqConnection;
import com.bytechef.component.rabbitmq.constant.RabbitMqConstants;
import com.bytechef.component.rabbitmq.trigger.RabbitMqNewMessageTrigger;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

import static com.bytechef.hermes.component.definition.ComponentDSL.component;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class RabbitMqComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(RabbitMqConstants.RABBIT_MQ)
        .title("RabbitMQ")
        .description(
            "RabbitMQ is an open-source message broker software that enables efficient communication between different systems, applications, and services. It supports multiple messaging protocols and facilitates a reliable and flexible messaging system.")
        .connection(RabbitMqConnection.CONNECTION_DEFINITION)
        .actions(RabbitMqSendMessageAction.ACTION_DEFINITION)
        .triggers(RabbitMqNewMessageTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
