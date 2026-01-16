/*
 * Copyright 2025 ByteChef
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

package com.bytechef.message.broker.jms.config;

import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.annotation.ConditionalOnMessageBrokerJms;
import com.bytechef.message.broker.jms.JmsMessageBroker;
import jakarta.jms.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jms.autoconfigure.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.JacksonJsonMessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnMessageBrokerJms
public class JmsMessageBrokerConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(JmsMessageBrokerConfiguration.class);

    public JmsMessageBrokerConfiguration() {
        if (logger.isDebugEnabled()) {
            logger.debug("Message broker provider type enabled: jms");
        }
    }

    @Bean
    MessageConverter jacksonJmsMessageConverter(JsonMapper objectMapper) {
        JacksonJsonMessageConverter jacksonJsonMessageConverter = new JacksonJsonMessageConverter(objectMapper);

        jacksonJsonMessageConverter.setTargetType(MessageType.TEXT);
        jacksonJsonMessageConverter.setTypeIdPropertyName("_type");

        return jacksonJsonMessageConverter;
    }

    @Bean
    JmsListenerContainerFactory<?> jmsListenerContainerFactory(
        ConnectionFactory connectionFactory,
        DefaultJmsListenerContainerFactoryConfigurer jmsListenerContainerFactoryConfigurer) {

        DefaultJmsListenerContainerFactory jmsListenerContainerFactory = new DefaultJmsListenerContainerFactory();

        jmsListenerContainerFactoryConfigurer.configure(jmsListenerContainerFactory, connectionFactory);

        return jmsListenerContainerFactory;
    }

    @Bean
    MessageBroker jmsMessageBroker(JmsTemplate jmsTemplate) {
        return new JmsMessageBroker(jmsTemplate);
    }
}
