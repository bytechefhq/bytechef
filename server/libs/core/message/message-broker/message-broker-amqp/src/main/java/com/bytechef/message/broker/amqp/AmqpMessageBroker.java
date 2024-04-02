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
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.message.broker.amqp;

import com.bytechef.message.Prioritizable;
import com.bytechef.message.Retryable;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.route.MessageRoute;
import org.apache.commons.lang3.Validate;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessageProperties;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class AmqpMessageBroker implements MessageBroker {

    private AmqpTemplate amqpTemplate;

    @Override
    public void send(MessageRoute messageRoute, Object message) {
        Validate.notNull(messageRoute, "'queueName' must not be null");

        amqpTemplate.convertAndSend(
            determineExchange(messageRoute.getName()), determineRoutingKey(messageRoute.getName()), message,
            amqpMessage -> {
                if (message instanceof Retryable retryable) {
                    MessageProperties messageProperties = amqpMessage.getMessageProperties();

                    messageProperties.setDelayLong(Long.valueOf(retryable.getRetryDelayMillis()));
                }

                if (message instanceof Prioritizable prioritizable) {
                    MessageProperties messageProperties = amqpMessage.getMessageProperties();

                    messageProperties.setPriority(prioritizable.getPriority());
                }

                return amqpMessage;
            });
    }

    private String determineExchange(String queueName) {
        String[] routingKeyItems = queueName.split("/");

        Validate.isTrue(routingKeyItems.length <= 2, "Invalid routing key: " + queueName);

        return routingKeyItems.length == 2 ? routingKeyItems[0] : MessageRoute.Exchange.MESSAGE.toString();
    }

    private String determineRoutingKey(String queueName) {
        String[] routingKeyItems = queueName.split("/");

        Validate.isTrue(routingKeyItems.length <= 2, "Invalid routing key: " + queueName);

        return routingKeyItems.length == 2 ? routingKeyItems[1] : queueName;
    }

    public void setAmqpTemplate(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }
}
