
/*
 * Copyright 2016-2018 the original author or authors.
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
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.message.broker.amqp;

import com.bytechef.atlas.message.broker.WorkflowExchange;
import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.priority.Prioritizable;
import com.bytechef.atlas.task.Retryable;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class AmqpMessageBroker implements MessageBroker {

    private AmqpTemplate amqpTemplate;

    @Override
    public void send(String queueName, Object message) {
        Assert.notNull(queueName, "'queueName' must not be null");

        amqpTemplate.convertAndSend(determineExchange(queueName), determineRoutingKey(queueName), message,
            amqpMessage -> {
                if (message instanceof Retryable) {
                    Retryable retryable = (Retryable) message;

                    MessageProperties messageProperties = amqpMessage.getMessageProperties();

                    messageProperties.setDelay((int) retryable.getRetryDelayMillis());
                }

                if (message instanceof Prioritizable) {
                    Prioritizable prioritizable = (Prioritizable) message;

                    MessageProperties messageProperties = amqpMessage.getMessageProperties();

                    messageProperties.setPriority(prioritizable.getPriority());
                }

                return amqpMessage;
            });
    }

    private String determineExchange(String queueName) {
        String[] routingKeyItems = queueName.split("/");

        Assert.isTrue(routingKeyItems.length <= 2, "Invalid routing key: " + queueName);

        return routingKeyItems.length == 2 ? routingKeyItems[0] : WorkflowExchange.TASKS.toString();
    }

    private String determineRoutingKey(String queueName) {
        String[] routingKeyItems = queueName.split("/");

        Assert.isTrue(routingKeyItems.length <= 2, "Invalid routing key: " + queueName);

        return routingKeyItems.length == 2 ? routingKeyItems[1] : queueName;
    }

    public void setAmqpTemplate(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }
}
