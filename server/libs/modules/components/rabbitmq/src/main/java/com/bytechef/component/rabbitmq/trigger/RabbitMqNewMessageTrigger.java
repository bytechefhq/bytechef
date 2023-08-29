
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

package com.bytechef.component.rabbitmq.trigger;

import com.bytechef.component.rabbitmq.util.RabbitMqUtils;
import com.bytechef.hermes.component.definition.Context.Connection;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.hermes.component.definition.TriggerDefinition.ListenerEmitter;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.component.util.JsonUtils;
import com.bytechef.hermes.component.util.MapUtils;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.bytechef.component.rabbitmq.constant.RabbitMqConstants.HOSTNAME;
import static com.bytechef.component.rabbitmq.constant.RabbitMqConstants.PASSWORD;
import static com.bytechef.component.rabbitmq.constant.RabbitMqConstants.PORT;
import static com.bytechef.component.rabbitmq.constant.RabbitMqConstants.QUEUE;
import static com.bytechef.component.rabbitmq.constant.RabbitMqConstants.USERNAME;
import static com.bytechef.hermes.component.definition.ComponentDSL.trigger;

import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class RabbitMqNewMessageTrigger {

    private static final Map<String, com.rabbitmq.client.Connection> CONNECTION_MAP = new ConcurrentHashMap<>();

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newMessage")
        .title("New Message")
        .description("Triggers on new RabbitMQ messages.")
        .type(TriggerType.LISTENER)
        .properties(
            string(QUEUE)
                .description("The name of the queue to read from")
                .required(true))
        .outputSchema(object())
        .listenerEnable(RabbitMqNewMessageTrigger::listenerEnable)
        .listenerDisable(RabbitMqNewMessageTrigger::listenerDisable);

    protected static void listenerEnable(
        Connection connection, Map<String, ?> inputParameters, String workflowExecutionId,
        ListenerEmitter listenerEmitter) {

        try {
            com.rabbitmq.client.Connection rabbitMqConnection = RabbitMqUtils.getConnection(
                MapUtils.getString(connection.getParameters(), HOSTNAME),
                MapUtils.getInteger(connection.getParameters(), PORT, 5672),
                MapUtils.getString(connection.getParameters(), USERNAME),
                MapUtils.getString(connection.getParameters(), PASSWORD));

            CONNECTION_MAP.put(workflowExecutionId, rabbitMqConnection);

            Channel channel = rabbitMqConnection.createChannel();

            channel.queueDeclare(MapUtils.getRequiredString(inputParameters, QUEUE), true, false, false, null);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

                listenerEmitter.emit(JsonUtils.read(message));
            };

            channel.basicConsume(MapUtils.getString(inputParameters, QUEUE), true, deliverCallback, consumerTag -> {});
        } catch (Exception e) {
            throw new ComponentExecutionException(e.getMessage(), e);
        }
    }

    protected static void listenerDisable(
        Connection connection, Map<String, ?> inputParameters, String workflowExecutionId) {

        com.rabbitmq.client.Connection rabbitmqConnection = CONNECTION_MAP.remove(workflowExecutionId);

        try {
            rabbitmqConnection.close();
        } catch (IOException e) {
            throw new ComponentExecutionException(e.getMessage(), e);
        }
    }
}
