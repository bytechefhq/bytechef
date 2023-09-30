
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

package com.bytechef.message.broker.jms.config;

import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.jms.JmsMessageBroker;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.ConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "message-broker.provider", havingValue = "jms")
public class JmsMessageBrokerConfiguration {

    @Bean
    MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter mappingJackson2MessageConverter = new MappingJackson2MessageConverter();

        mappingJackson2MessageConverter.setObjectMapper(objectMapper);
        mappingJackson2MessageConverter.setTargetType(MessageType.TEXT);
        mappingJackson2MessageConverter.setTypeIdPropertyName("_type");

        return mappingJackson2MessageConverter;
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
