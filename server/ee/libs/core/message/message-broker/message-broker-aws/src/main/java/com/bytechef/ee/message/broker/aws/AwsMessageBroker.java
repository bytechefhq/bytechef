/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.message.broker.aws;

import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.route.MessageRoute;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AwsMessageBroker implements MessageBroker {

    @Override
    public void send(MessageRoute route, Object message) {
        // TODO
    }
}
