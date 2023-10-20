
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

package com.bytechef.message.broker.jms;

import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.Retryable;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 */
public class JmsMessageBroker implements MessageBroker {

    private final JmsTemplate jmsTemplate;

    @SuppressFBWarnings("EI")
    public JmsMessageBroker(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    public void send(String queueName, Object message) {
        Assert.notNull(queueName, "'queueName' must not be null");

        if (message instanceof Retryable) {
            Retryable retryable = (Retryable) message;

            delay(retryable.getRetryDelayMillis());
        }

        jmsTemplate.convertAndSend(queueName, message);
    }

    private void delay(long value) {
        try {
            TimeUnit.MILLISECONDS.sleep(value);
        } catch (InterruptedException e) {
        }
    }
}
