
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

package com.bytechef.atlas.message.broker.redis.listener;

import com.bytechef.atlas.message.broker.Exchanges;
import com.bytechef.atlas.message.broker.redis.serializer.RedisMessageSerializer;
import com.oblac.jrsmq.QueueMessage;
import com.oblac.jrsmq.RedisSMQ;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.util.MethodInvoker;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Ivica Cardic
 */
public class RedisListenerEndpointRegistrar implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisListenerEndpointRegistrar.class);

    private final ExecutorService executorService;
    private final Map<String, Consumer<String>> invokerMap = new HashMap<>();
    private final RedisMessageSerializer redisMessageSerializer;
    private final RedisSMQ redisSMQ;
    private boolean stopped;

    @SuppressFBWarnings("EI2")
    public RedisListenerEndpointRegistrar(
        ExecutorService executorService, RedisMessageSerializer redisMessageSerializer, RedisSMQ redisSMQ) {

        this.executorService = executorService;
        this.redisMessageSerializer = redisMessageSerializer;
        this.redisSMQ = redisSMQ;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        Consumer<String> invokerConsumer = invokerMap.get(new String(message.getChannel(), StandardCharsets.UTF_8));

        invokerConsumer.accept(message.toString());
    }

    public void registerListenerEndpoint(String queueName, Object delegate, String methodName, Exchanges exchanges) {
        invokerMap.put(queueName, (String message) -> invoke(delegate, methodName, message));

        checkQueueExists(queueName);

        if (exchanges == Exchanges.TASKS) {
            executorService.submit(() -> periodicallyCheckQueueForMessage(queueName));
        }
    }

    public void stop() {
        this.stopped = true;
    }

    private void periodicallyCheckQueueForMessage(String queueName) {
        while (!stopped) {
            QueueMessage queueMessage = redisSMQ.popMessage()
                .qname(queueName)
                .exec();

            if (queueMessage == null) {
                sleep();
            } else {
                Consumer<String> invokerConsumer = invokerMap.get(queueName);

                executorService.submit(() -> invokerConsumer.accept(queueMessage.message()));
            }
        }
    }

    private void checkQueueExists(String queueName) {
        Set<String> queueNames = redisSMQ.listQueues()
            .exec();

        if (!queueNames.contains(queueName)) {
            redisSMQ.createQueue()
                .qname(queueName)
                .exec();
        }
    }

    private void invoke(Object delegate, String methodName, String messageString) {
        Object message = redisMessageSerializer.deserialize(messageString);

        try {
            MethodInvoker methodInvoker = new MethodInvoker();

            methodInvoker.setTargetObject(delegate);
            methodInvoker.setTargetMethod(methodName);
            methodInvoker.setArguments(message);

            methodInvoker.prepare();

            methodInvoker.invoke();
        } catch (Exception e) {
            if (!stopped) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private void sleep() {
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException ex) {
            // ignore
        }
    }
}
