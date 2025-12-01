/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.message.broker.memory.config;

import com.bytechef.message.broker.annotation.ConditionalOnMessageBrokerMemory;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.broker.config.MessageBrokerListenerRegistrar;
import com.bytechef.message.broker.memory.MemoryMessageBroker;
import com.bytechef.message.broker.memory.listener.MemoryListenerEndpointRegistrar;
import com.bytechef.message.route.MessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnMessageBrokerMemory
public class MemoryMessageBrokerListenerRegistrarConfiguration
    implements
    MessageBrokerListenerRegistrar<MemoryListenerEndpointRegistrar>,
    SmartInitializingSingleton {

    private static final Logger logger = LoggerFactory.getLogger(
        MemoryMessageBrokerListenerRegistrarConfiguration.class);

    private final List<MessageBrokerConfigurer<MemoryListenerEndpointRegistrar>> messageBrokerConfigurers;
    private final MemoryMessageBroker memoryMessageBroker;
    private MemoryListenerEndpointRegistrar listenerEndpointRegistrar;

    @SuppressFBWarnings("EI")
    public MemoryMessageBrokerListenerRegistrarConfiguration(
        @Autowired(
            required = false) List<MessageBrokerConfigurer<MemoryListenerEndpointRegistrar>> messageBrokerConfigurers,
        MemoryMessageBroker memoryMessageBroker) {

        this.memoryMessageBroker = memoryMessageBroker;
        this.messageBrokerConfigurers = messageBrokerConfigurers == null ? List.of() : messageBrokerConfigurers;
    }

    @Override
    public void afterSingletonsInstantiated() {
        listenerEndpointRegistrar = new MemoryListenerEndpointRegistrar(memoryMessageBroker);

        for (MessageBrokerConfigurer<MemoryListenerEndpointRegistrar> messageBrokerConfigurer : messageBrokerConfigurers) {

            messageBrokerConfigurer.configure(listenerEndpointRegistrar, this);
        }
    }

    @Override
    public void registerListenerEndpoint(
        MemoryListenerEndpointRegistrar listenerEndpointRegistrar, MessageRoute messageRoute, int concurrency,
        Object delegate, String methodName) {

        Class<?> delegateClass = delegate.getClass();

        if (logger.isTraceEnabled()) {
            logger.trace("Registering Local Listener: {} -> {}:{}", messageRoute, delegateClass, methodName);
        }

        listenerEndpointRegistrar.registerListenerEndpoint(messageRoute, delegate, methodName);
    }

    @Override
    public void stopListenerEndpoints() {
        if (listenerEndpointRegistrar != null) {
            listenerEndpointRegistrar.stop();
        }
    }

    @Override
    public void startListenerEndpoints() {
        if (listenerEndpointRegistrar != null) {
            listenerEndpointRegistrar.start();
        }
    }
}
