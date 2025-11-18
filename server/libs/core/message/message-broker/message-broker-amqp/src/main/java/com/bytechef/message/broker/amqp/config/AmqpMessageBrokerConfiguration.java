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

package com.bytechef.message.broker.amqp.config;

import com.bytechef.message.broker.amqp.AmqpMessageBroker;
import com.bytechef.message.broker.annotation.ConditionalOnMessageBrokerAmqp;
import com.bytechef.message.route.SystemMessageRoute;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnMessageBrokerAmqp
public class AmqpMessageBrokerConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AmqpMessageBrokerConfiguration.class);

    public AmqpMessageBrokerConfiguration() {
        if (logger.isDebugEnabled()) {
            logger.debug("Message broker provider type enabled: amqp");
        }
    }

    @Bean
    AmqpMessageBroker amqpMessageBroker(AmqpTemplate amqpTemplate) {
        AmqpMessageBroker amqpMessageBroker = new AmqpMessageBroker();

        amqpMessageBroker.setAmqpTemplate(amqpTemplate);

        return amqpMessageBroker;
    }

    @Bean
    Queue dlqQueue() {
        return new Queue(SystemMessageRoute.DLQ.toString());
    }

    @Bean
    MessageConverter jacksonAmqpMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
