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
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.message.broker.sync.config;

import com.bytechef.message.broker.annotation.ConditionalOnMessageBrokerLocal;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.broker.config.MessageBrokerListenerRegistrar;
import com.bytechef.message.broker.sync.SyncMessageBroker;
import com.bytechef.message.broker.sync.listener.LocalListenerEndpointRegistrar;
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
@ConditionalOnMessageBrokerLocal
public class LocalMessageBrokerListenerRegistrarConfiguration
    implements
    MessageBrokerListenerRegistrar<LocalListenerEndpointRegistrar>,
    SmartInitializingSingleton {

    private static final Logger logger = LoggerFactory.getLogger(
        LocalMessageBrokerListenerRegistrarConfiguration.class);

    private final List<MessageBrokerConfigurer<LocalListenerEndpointRegistrar>> messageBrokerConfigurers;
    private final SyncMessageBroker syncMessageBroker;

    @SuppressFBWarnings("EI")
    public LocalMessageBrokerListenerRegistrarConfiguration(
        @Autowired(
            required = false) List<MessageBrokerConfigurer<LocalListenerEndpointRegistrar>> messageBrokerConfigurers,
        SyncMessageBroker syncMessageBroker) {

        this.syncMessageBroker = syncMessageBroker;
        this.messageBrokerConfigurers = messageBrokerConfigurers == null ? List.of() : messageBrokerConfigurers;
    }

    @Override
    public void afterSingletonsInstantiated() {
        LocalListenerEndpointRegistrar listenerEndpointRegistrar = new LocalListenerEndpointRegistrar(
            syncMessageBroker);

        for (MessageBrokerConfigurer<LocalListenerEndpointRegistrar> messageBrokerConfigurer : messageBrokerConfigurers) {

            messageBrokerConfigurer.configure(listenerEndpointRegistrar, this);
        }
    }

    @Override
    public void registerListenerEndpoint(
        LocalListenerEndpointRegistrar listenerEndpointRegistrar, MessageRoute messageRoute, int concurrency,
        Object delegate, String methodName) {

        Class<?> delegateClass = delegate.getClass();

        logger.info("Registering Local Listener: {} -> {}:{}", messageRoute, delegateClass, methodName);

        listenerEndpointRegistrar.registerListenerEndpoint(messageRoute, delegate, methodName);
    }
}
