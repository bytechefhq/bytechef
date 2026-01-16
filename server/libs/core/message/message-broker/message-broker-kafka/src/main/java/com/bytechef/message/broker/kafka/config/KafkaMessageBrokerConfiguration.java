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

package com.bytechef.message.broker.kafka.config;

import com.bytechef.message.broker.MessageBroker;
import com.bytechef.message.broker.annotation.ConditionalOnMessageBrokerKafka;
import com.bytechef.message.broker.kafka.KafkaMessageBroker;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.jackson.JsonValueSerializer;

/**
 * @author Arik Cohen
 */
@Configuration
@ConditionalOnMessageBrokerKafka
public class KafkaMessageBrokerConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageBrokerConfiguration.class);

    public KafkaMessageBrokerConfiguration() {
        if (logger.isDebugEnabled()) {
            logger.debug("Message broker provider type enabled: kafka");
        }
    }

    @Bean
    MessageBroker kafkaMessageBroker(KafkaTemplate<String, Object> kafkaTemplate) {
        KafkaMessageBroker kafkaMessageBroker = new KafkaMessageBroker();

        kafkaMessageBroker.setKafkaTemplate(kafkaTemplate);

        return kafkaMessageBroker;
    }

    @Bean
    KafkaTemplate<String, Object> kafkaTemplate(JsonMapper objectMapper, KafkaProperties kafkaProperties) {
        return new KafkaTemplate<>(producerFactory(objectMapper, kafkaProperties));
    }

    @Bean
    ProducerFactory<String, Object> producerFactory(JsonMapper objectMapper, KafkaProperties kafkaProperties) {
        return new DefaultKafkaProducerFactory<>(
            producerConfigs(kafkaProperties), new StringSerializer(), new JacksonJsonSerializer<>(objectMapper));
    }

    @Bean
    Map<String, Object> producerConfigs(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonValueSerializer.class);

        return props;
    }

    @Bean
    Map<String, Object> consumerConfigs(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();

        return props;
    }

    @Bean
    MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        JacksonJsonMessageConverter jacksonJsonMessageConverter = new JacksonJsonMessageConverter() {

            @Override
            protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
                MessageHeaders messageHeaders = message.getHeaders();

                String type = (String) messageHeaders.get("_type");

                if (type != null) {
                    try {
                        targetClass = Class.forName(type);
                    } catch (ClassNotFoundException e) {
                        logger.warn("Class not found: " + type, e);
                    }
                }

                JavaType javaType = objectMapper.constructType(targetClass);
                Object payload = message.getPayload();
                Class<?> view = getSerializationView(conversionHint);

                // Note: in the view case, calling withType instead of forType for
                // compatibility with Jackson <2.5

                if (payload instanceof byte[]) {
                    if (view != null) {
                        return objectMapper.readerWithView(view)
                            .forType(javaType)
                            .readValue((byte[]) payload);
                    } else {
                        return objectMapper.readValue((byte[]) payload, javaType);
                    }
                } else {
                    if (view != null) {
                        return objectMapper.readerWithView(view)
                            .forType(javaType)
                            .readValue(payload.toString());
                    } else {
                        return objectMapper.readValue(payload.toString(), javaType);
                    }
                }
            }
        };

        return jacksonJsonMessageConverter;
    }

    @Bean
    MessageHandlerMethodFactory messageHandlerMethodFactory(MessageConverter messageConverter) {
        DefaultMessageHandlerMethodFactory messageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();
        messageHandlerMethodFactory.setMessageConverter(messageConverter);

        return messageHandlerMethodFactory;
    }
}
