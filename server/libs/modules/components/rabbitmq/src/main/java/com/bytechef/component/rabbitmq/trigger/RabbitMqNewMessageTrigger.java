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

package com.bytechef.component.rabbitmq.trigger;

import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.trigger;
import static com.bytechef.component.rabbitmq.constant.RabbitMqConstants.HOSTNAME;
import static com.bytechef.component.rabbitmq.constant.RabbitMqConstants.PASSWORD;
import static com.bytechef.component.rabbitmq.constant.RabbitMqConstants.PORT;
import static com.bytechef.component.rabbitmq.constant.RabbitMqConstants.QUEUE;
import static com.bytechef.component.rabbitmq.constant.RabbitMqConstants.USERNAME;

import com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerDefinition.ListenerEmitter;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerOutputFunction.ListenerTriggerOutputFunction;
import com.bytechef.component.rabbitmq.util.RabbitMqUtils;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * @author Ivica Cardic
 */
public class RabbitMqNewMessageTrigger {

    private static final Map<String, Connection> CONNECTION_MAP = new ConcurrentHashMap<>();

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newMessage")
        .title("New Message")
        .description("Triggers on new RabbitMQ messages.")
        .type(TriggerType.LISTENER)
        .properties(
            string(QUEUE)
                .description("The name of the queue to read from")
                .required(true))
        .output(getOutputSchema())
        .listenerEnable(RabbitMqNewMessageTrigger::listenerEnable)
        .listenerDisable(RabbitMqNewMessageTrigger::listenerDisable);

    protected static ListenerTriggerOutputFunction getOutputSchema() {
        // TODO

        return (inputParameters, connectionParameters, workflowExecutionId, context) -> null;
    }

    protected static void listenerDisable(
        Parameters inputParameters, Parameters connectionParameters, String workflowExecutionId,
        Context context) throws IOException {

        Connection rabbitmqConnection = CONNECTION_MAP.remove(workflowExecutionId);

        rabbitmqConnection.close();
    }

    protected static void listenerEnable(
        Parameters inputParameters, Parameters connectionParameters, String workflowExecutionId,
        ListenerEmitter listenerEmitter, Context context) throws IOException, TimeoutException {

        Connection rabbitMqConnection = RabbitMqUtils.getConnection(
            connectionParameters.getString(HOSTNAME), connectionParameters.getInteger(PORT, 5672),
            connectionParameters.getString(USERNAME), connectionParameters.getString(PASSWORD));

        CONNECTION_MAP.put(workflowExecutionId, rabbitMqConnection);

        Channel channel = rabbitMqConnection.createChannel();

        channel.queueDeclare(inputParameters.getRequiredString(QUEUE), true, false, false, null);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

            listenerEmitter.emit(context.json(json -> json.readMap(message)));
        };

        channel.basicConsume(inputParameters.getString(QUEUE), true, deliverCallback, consumerTag -> {});
    }
}
