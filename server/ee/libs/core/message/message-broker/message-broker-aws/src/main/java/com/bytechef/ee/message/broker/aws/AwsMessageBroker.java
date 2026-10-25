/*
 * Copyright 2025 ByteChef
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
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AwsMessageBroker implements MessageBroker {

    private final @Nullable RedisControlSender redisControlSender;
    private final SqsTemplate sqsTemplate;

    @SuppressFBWarnings({
        "EI", "EI2"
    })
    public AwsMessageBroker(SqsTemplate sqsTemplate, @Nullable RedisControlSender redisControlSender) {
        this.sqsTemplate = sqsTemplate;
        this.redisControlSender = redisControlSender;
    }

    @Override
    public void send(MessageRoute route, Object message) {
        String routeName = route.getName();

        if (redisControlSender != null && route.isControlExchange()) {
            redisControlSender.send(routeName, message);

            return;
        }

        String modifiedRoute = routeName.replace(".", "-");

        sqsTemplate.sendAsync(to -> {
            SqsSendOptions<Object> queue = to.queue(modifiedRoute);

            queue.payload(message);
        });
    }
}
