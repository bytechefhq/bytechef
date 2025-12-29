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

package com.bytechef.message.broker.kafka;

import com.bytechef.message.Retryable;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.route.MessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 */
public class KafkaMessageBroker implements MessageBroker {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageBroker.class);

    private KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void send(MessageRoute messageRoute, Object message) {
        Assert.notNull(messageRoute, "'queueName' key must not be null");

        if (message instanceof Retryable retryable) {
            delay(retryable.getRetryDelayMillis());
        }

        Class<?> messageClass = message.getClass();

        kafkaTemplate.send(MessageBuilder.withPayload(message)
            .setHeader(KafkaHeaders.TOPIC, messageRoute)
            .setHeader("_type", messageClass.getName())
            .build());
    }

    private void delay(long aValue) {
        try {
            TimeUnit.MILLISECONDS.sleep(aValue);
        } catch (InterruptedException e) {
            if (logger.isTraceEnabled()) {
                logger.trace(e.getMessage(), e);
            }
        }
    }

    @SuppressFBWarnings("EI")
    public void setKafkaTemplate(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
}
