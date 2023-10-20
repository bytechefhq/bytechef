
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

package com.bytechef.atlas.message.broker.redis;

import com.bytechef.atlas.message.broker.MessageBroker;
import com.bytechef.atlas.task.Retryable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @author Ivica Cardic
 */
public class RedisMessageBroker implements MessageBroker {

    private final RedisTemplate<String, Object> redisTemplate;

    @SuppressFBWarnings("EI2")
    public RedisMessageBroker(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void send(String routingKey, Object message) {
        ListOperations<String, Object> listOperations = redisTemplate.opsForList();

        if (message instanceof Retryable retryable) {
            delay(retryable.getRetryDelayMillis());

            listOperations.leftPush(routingKey, message);
        } else {
            listOperations.leftPush(routingKey, message);
        }
    }

    private void delay(long value) {
        try {
            TimeUnit.MILLISECONDS.sleep(value);
        } catch (InterruptedException e) {
        }
    }
}
