
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

package com.bytechef.component.rabbitmq.action;

import com.bytechef.component.rabbitmq.util.RabbitMqUtils;
import com.bytechef.hermes.component.ActionContext;
import com.bytechef.hermes.component.Context.Connection;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.component.util.JsonUtils;
import com.bytechef.hermes.component.util.MapValueUtils;
import com.rabbitmq.client.Channel;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.bytechef.component.rabbitmq.constant.RabbitMqConstants.HOSTNAME;
import static com.bytechef.component.rabbitmq.constant.RabbitMqConstants.MESSAGE;
import static com.bytechef.component.rabbitmq.constant.RabbitMqConstants.PASSWORD;
import static com.bytechef.component.rabbitmq.constant.RabbitMqConstants.PORT;
import static com.bytechef.component.rabbitmq.constant.RabbitMqConstants.QUEUE;
import static com.bytechef.component.rabbitmq.constant.RabbitMqConstants.USERNAME;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;

import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class RabbitMqSendMessageAction {

    public static final ActionDefinition ACTION_DEFINITION = action("sendMessage")
        .title("Send Message")
        .description("Send a new RabbitMQ message.")
        .properties(
            string(QUEUE)
                .description("The name of the queue to read from")
                .required(true),
            oneOf(MESSAGE)
                .description("The name of the queue to read from")
                .required(true))
        .execute(RabbitMqSendMessageAction::execute);

    protected static Object execute(ActionContext context, Map<String, ?> inputParameters) {
        Connection connection = context.getConnection();

        try (com.rabbitmq.client.Connection rabbitMqConnection = RabbitMqUtils.getConnection(
            MapValueUtils.getString(connection.getParameters(), HOSTNAME),
            MapValueUtils.getInteger(connection.getParameters(), PORT, 5672),
            MapValueUtils.getString(connection.getParameters(), USERNAME),
            MapValueUtils.getString(connection.getParameters(), PASSWORD))) {

            Channel channel = rabbitMqConnection.createChannel();

            String queueName = MapValueUtils.getRequiredString(inputParameters, QUEUE);
            String message = JsonUtils.write(MapValueUtils.getRequired(inputParameters, MESSAGE));

            channel.queueDeclare(queueName, true, false, false, null);
            channel.basicPublish("", queueName, null, message.getBytes(StandardCharsets.UTF_8));

            channel.close();
        } catch (Exception e) {
            throw new ComponentExecutionException(e.getMessage(), e);
        }

        return null;
    }
}
