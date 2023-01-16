
/*
 * Copyright 2016-2018 the original author or authors.
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
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.message.broker.kafka.config;

import com.bytechef.atlas.message.broker.Exchanges;
import com.bytechef.atlas.message.broker.Queues;
import com.bytechef.atlas.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.atlas.message.broker.config.MessageBrokerListenerRegistrar;
import com.bytechef.atlas.message.broker.kafka.KafkaMessageBroker;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListenerConfigurer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar;
import org.springframework.kafka.config.MethodKafkaListenerEndpoint;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

/**
 * @author Arik Cohen
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.workflow", name = "message-broker.provider", havingValue = "kafka")
public class KafkaMessageBrokerConfiguration
    implements KafkaListenerConfigurer, MessageBrokerListenerRegistrar<KafkaListenerEndpointRegistrar> {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageBrokerConfiguration.class);

    @Autowired
    private BeanFactory beanFactory;

    @Autowired(required = false)
    private List<MessageBrokerConfigurer> messageBrokerConfigurers = Collections.emptyList();

    @Autowired
    private MessageHandlerMethodFactory messageHandlerMethodFactory;

    @Autowired
    private Map<String, Object> consumerConfigs;

    @Override
    @SuppressWarnings("unchecked")
    public void configureKafkaListeners(KafkaListenerEndpointRegistrar listenerEndpointRegistrar) {
        for (MessageBrokerConfigurer messageBrokerConfigurer : messageBrokerConfigurers) {
            messageBrokerConfigurer.configure(listenerEndpointRegistrar, this);
        }
    }

    @Override
    public void registerListenerEndpoint(
        KafkaListenerEndpointRegistrar listenerEndpointRegistrar, String queueName, int concurrency, Object delegate,
        String methodName) {

        if (Objects.equals(queueName, Queues.CONTROL)) {
            queueName = Exchanges.CONTROL;
        }

        Class<?> delegateClass = delegate.getClass();

        logger.info("Registering KAFKA Listener: {} -> {}:{}", queueName, delegateClass.getName(), methodName);

        Method listenerMethod = Stream.of(delegate.getClass()
            .getMethods())
            .filter(it -> methodName.equals(it.getName()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("No method found: " + methodName + " on " + delegate.getClass()));

        MethodKafkaListenerEndpoint<String, String> endpoint = createListenerEndpoint(
            queueName, delegate, listenerMethod);

        listenerEndpointRegistrar.registerEndpoint(endpoint);
    }

    @Bean
    KafkaMessageBroker kafkaMessageBroker(KafkaTemplate kafkaTemplate) {
        KafkaMessageBroker kafkaMessageBroker = new KafkaMessageBroker();

        kafkaMessageBroker.setKafkaTemplate(kafkaTemplate);

        return kafkaMessageBroker;
    }

    @Bean
    KafkaTemplate<String, Object> kafkaTemplate(ObjectMapper objectMapper, KafkaProperties kafkaProperties) {
        KafkaTemplate<String, Object> kafkaTemplate = new KafkaTemplate<>(
            producerFactory(objectMapper, kafkaProperties));

        return kafkaTemplate;
    }

    @Bean
    ProducerFactory<String, Object> producerFactory(ObjectMapper objectMapper, KafkaProperties kafkaProperties) {
        return new DefaultKafkaProducerFactory<>(
            producerConfigs(kafkaProperties), new StringSerializer(), new JsonSerializer<>(objectMapper));
    }

    @Bean
    Map<String, Object> producerConfigs(KafkaProperties aKafkaProperties) {
        Map<String, Object> props = aKafkaProperties.buildProducerProperties();

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return props;
    }

    @Bean
    Map<String, Object> consumerConfigs(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();

        return props;
    }

    @Bean
    MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter mappingJackson2MessageConverter = new MappingJackson2MessageConverter() {

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

                JavaType javaType = getObjectMapper().constructType(targetClass);
                Object payload = message.getPayload();
                Class<?> view = getSerializationView(conversionHint);

                // Note: in the view case, calling withType instead of forType for
                // compatibility with Jackson <2.5
                try {
                    if (payload instanceof byte[]) {
                        if (view != null) {
                            return getObjectMapper()
                                .readerWithView(view)
                                .forType(javaType)
                                .readValue((byte[]) payload);
                        } else {
                            return getObjectMapper().readValue((byte[]) payload, javaType);
                        }
                    } else {
                        if (view != null) {
                            return getObjectMapper()
                                .readerWithView(view)
                                .forType(javaType)
                                .readValue(payload.toString());
                        } else {
                            return getObjectMapper().readValue(payload.toString(), javaType);
                        }
                    }
                } catch (IOException ex) {
                    throw new MessageConversionException(message, "Could not read JSON: " + ex.getMessage(), ex);
                }
            }
        };

        mappingJackson2MessageConverter.setObjectMapper(objectMapper);

        return mappingJackson2MessageConverter;
    }

    @Bean
    MessageHandlerMethodFactory messageHandlerMethodFactory(MessageConverter messageConverter) {
        DefaultMessageHandlerMethodFactory messageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();
        messageHandlerMethodFactory.setMessageConverter(messageConverter);

        return messageHandlerMethodFactory;
    }

    private KafkaListenerContainerFactory createContainerFactory(int concurrency) {
        ConcurrentKafkaListenerContainerFactory factory = new ConcurrentKafkaListenerContainerFactory();

        factory.setConcurrency(concurrency);

        return factory;
    }

    private MethodKafkaListenerEndpoint<String, String> createListenerEndpoint(
        String queueName, Object listener, Method listenerMethod) {

        final MethodKafkaListenerEndpoint<String, String> endpoint = new MethodKafkaListenerEndpoint<>();

        endpoint.setBeanFactory(beanFactory);
        endpoint.setBean(listener);
        endpoint.setMethod(listenerMethod);
        endpoint.setId(queueName + "Endpoint");
        endpoint.setTopics(queueName);
        endpoint.setMessageHandlerMethodFactory(messageHandlerMethodFactory);

        return endpoint;
    }
}
