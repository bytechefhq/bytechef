/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.message.broker.aws;

import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.route.MessageRoute;
import io.awspring.cloud.sqs.operations.SqsTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AwsMessageBroker implements MessageBroker {
    private final SqsTemplate sqsTemplate;

    public AwsMessageBroker(SqsTemplate sqsTemplate) {
        this.sqsTemplate = sqsTemplate;
    }

    @Override
    public void send(MessageRoute route, Object message) {
        sqsTemplate.sendAsync(to -> to.queue(route.getName())
            .payload(message));
    }
}
