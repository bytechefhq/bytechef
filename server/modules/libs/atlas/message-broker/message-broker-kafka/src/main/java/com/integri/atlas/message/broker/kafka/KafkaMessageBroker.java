/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.message.broker.kafka;

import com.integri.atlas.engine.core.message.broker.MessageBroker;
import com.integri.atlas.engine.core.task.Retryable;
import java.util.concurrent.TimeUnit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 */
public class KafkaMessageBroker implements MessageBroker {

    private KafkaTemplate<Integer, Object> kafkaTemplate;

    @Override
    public void send(String aRoutingKey, Object aMessage) {
        Assert.notNull(aRoutingKey, "routing key can't be null");
        if (aMessage instanceof Retryable) {
            Retryable r = (Retryable) aMessage;
            delay(r.getRetryDelayMillis());
        }

        kafkaTemplate.send(
            MessageBuilder
                .withPayload(aMessage)
                .setHeader(KafkaHeaders.TOPIC, aRoutingKey)
                .setHeader("_type", aMessage.getClass().getName())
                .build()
        );
    }

    private void delay(long aValue) {
        try {
            TimeUnit.MILLISECONDS.sleep(aValue);
        } catch (InterruptedException e) {}
    }

    public void setKafkaTemplate(KafkaTemplate<Integer, Object> akafkaTemplate) {
        kafkaTemplate = akafkaTemplate;
    }
}
