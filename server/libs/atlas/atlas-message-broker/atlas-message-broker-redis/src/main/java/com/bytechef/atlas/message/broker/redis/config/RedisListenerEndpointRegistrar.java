
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

package com.bytechef.atlas.message.broker.redis.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * @author Ivica Cardic
 */
class RedisListenerEndpointRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(RedisListenerEndpointRegistrar.class);

    private final ExecutorService executorService;
    private final RedisTemplate<String, RedisMessage> redisTemplate;
    private boolean stopped;

    RedisListenerEndpointRegistrar(ExecutorService executorService, RedisTemplate<String, RedisMessage> redisTemplate) {
        this.executorService = executorService;
        this.redisTemplate = redisTemplate;
    }

    public void registerListenerEndpoint(String queueName, int concurrency, Object delegate, String methodName) {
        ListOperations<String, RedisMessage> listOperations = redisTemplate.opsForList();

        executorService.submit(() -> execute(queueName, delegate, methodName, listOperations));
    }

    private void execute(
        String queueName, Object delegate, String methodName, ListOperations<String, RedisMessage> listOperations) {

        while (!stopped) {
            try {
                RedisMessage redisMessage = listOperations.rightPop(queueName, Duration.ofSeconds(1));

                if (redisMessage != null) {
                    Method method = getMethod(delegate, methodName, redisMessage.getClass());

                    method.invoke(delegate, redisMessage.payload());
                }
            } catch (Exception e) {
                if (!stopped) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    private Method getMethod(Object delegate, String methodName, Class<?> messageClass) {
        Method method = null;

        Class<?> delegateClass = delegate.getClass();

        for (Method curMethod : delegateClass.getDeclaredMethods()) {
            if (Objects.equals(curMethod.getName(), methodName)) {
                Class<?>[] parameterTypes = curMethod.getParameterTypes();

                method = curMethod;

                if (messageClass.equals(parameterTypes[0])) {
                    break;
                }
            }
        }

        if (method == null) {
            throw new RuntimeException(
                "Method %s does not exist in class %s".formatted(methodName, delegateClass));
        }

        return method;
    }

    public void stop() {
        this.stopped = true;
    }
}
