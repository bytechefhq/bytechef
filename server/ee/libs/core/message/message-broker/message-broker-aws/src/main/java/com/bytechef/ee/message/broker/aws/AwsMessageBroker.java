/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.message.broker.aws;

import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.route.MessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.awspring.cloud.sqs.operations.SqsSendOptions;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.messaging.Message;

import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AwsMessageBroker implements MessageBroker {

    private final SqsTemplate sqsTemplate;

    @SuppressFBWarnings("EI")
    public AwsMessageBroker(SqsTemplate sqsTemplate) {
        this.sqsTemplate = sqsTemplate;
    }

    @Override
    public void send(MessageRoute route, Object message) {
        sqsTemplate.sendAsync(to -> {
            SqsSendOptions<Object> queue = to.queue(route.getName());

            queue.payload(message);
        });
    }

    public <T> Optional<T> receive(String queueName, Class<T> clazz){
        Optional<Message<T>> message = sqsTemplate.receive(from -> from.queue(queueName), clazz);

        return message.map(Message::getPayload);
    }
}
