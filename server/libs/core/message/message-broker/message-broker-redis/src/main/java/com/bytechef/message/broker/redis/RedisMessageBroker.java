
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.message.broker.redis;

import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.redis.serializer.RedisMessageSerializer;
import com.bytechef.message.Retryable;
import com.bytechef.message.route.MessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.oblac.jrsmq.RedisSMQ;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;

/**
 * @author Ivica Cardic
 */
public class RedisMessageBroker implements MessageBroker {

    private final RedisMessageSerializer redisMessageSerializer;
    private final RedisSMQ redisSMQ;
    private final StringRedisTemplate stringRedisTemplate;

    @SuppressFBWarnings("EI2")
    public RedisMessageBroker(
        RedisMessageSerializer redisMessageSerializer, RedisSMQ redisSMQ, StringRedisTemplate stringRedisTemplate) {

        this.redisMessageSerializer = redisMessageSerializer;
        this.redisSMQ = redisSMQ;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void send(MessageRoute messageRoute, Object message) {
        Assert.notNull(messageRoute, "'messageRoute' must not be null");

        if (message instanceof Retryable retryable) {
            delay(retryable.getRetryDelayMillis());
        }

        if (messageRoute.isControlExchange()) {
            sendMessageToTopic(messageRoute.getName(), message);
        } else {
            sendMessageToQueue(messageRoute.getName(), message);
        }
    }

    private void sendMessageToQueue(String queueName, Object message) {
        redisSMQ.sendMessage()
            .qname(queueName)
            .message(redisMessageSerializer.serialize(message))
            .exec();
    }

    private void sendMessageToTopic(String queueName, Object message) {
        stringRedisTemplate.convertAndSend(queueName, redisMessageSerializer.serialize(message));
    }

    private void delay(long value) {
        try {
            TimeUnit.MILLISECONDS.sleep(value);
        } catch (InterruptedException e) {
        }
    }
}
