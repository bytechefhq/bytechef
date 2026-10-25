/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.message.broker.aws;

/**
 * Publishes a CONTROL route message to every node over Redis pub/sub.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface RedisControlSender {

    void send(String channelName, Object message);
}
