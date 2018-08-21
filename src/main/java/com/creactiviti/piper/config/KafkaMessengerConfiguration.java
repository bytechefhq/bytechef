/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.creactiviti.piper.config;

import com.creactiviti.piper.core.Coordinator;
import com.creactiviti.piper.core.Worker;
import com.creactiviti.piper.core.event.EventListener;
import com.creactiviti.piper.core.messenger.Exchanges;
import com.creactiviti.piper.core.messenger.KafkaMessenger;
import com.creactiviti.piper.core.messenger.Queues;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Stream;

@Configuration
@EnableConfigurationProperties(PiperProperties.class)
@ConditionalOnProperty(name = "piper.messenger.provider", havingValue = "kafka")
public class KafkaMessengerConfiguration implements KafkaListenerConfigurer {

    @Autowired(required = false)
    private Worker worker;

    @Autowired(required = false)
    private Coordinator coordinator;

    @Autowired
    private EventListener eventListener;

    @Autowired
    private PiperProperties properties;

    @Autowired
    private BeanFactory beanFactory;

    @Autowired
    MessageHandlerMethodFactory messageHandlerMethodFactory;

    @Autowired
    Map<String, Object> consumerConfigs;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Bean
    KafkaMessenger kafkaMessenger(KafkaTemplate akafkaTemplate) {
        KafkaMessenger kafkaMessenger = new KafkaMessenger();
        kafkaMessenger.setKafkaTemplate(akafkaTemplate);
        return kafkaMessenger;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ObjectMapper aObjectMapper, KafkaProperties aKafkaProperties) {
        KafkaTemplate<String, Object> kafkaTemplate = new KafkaTemplate<>(producerFactory(aObjectMapper, aKafkaProperties));

        return kafkaTemplate;
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory(ObjectMapper aObjectMapper, KafkaProperties aKafkaProperties) {
        return new DefaultKafkaProducerFactory<>(
                producerConfigs(aKafkaProperties),
                new StringSerializer(),
                new JsonSerializer<>(aObjectMapper));
    }

    @Bean
    public Map<String, Object> producerConfigs(KafkaProperties aKafkaProperties) {
        Map<String, Object> props = aKafkaProperties.buildProducerProperties();

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return props;
    }

    @Bean
    public Map<String, Object> consumerConfigs(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();

        return props;
    }

    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper aObjectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter() {
            @Override
            protected Object convertFromInternal(Message<?> message, Class<?> targetClass, Object conversionHint) {
                String type = (String) message.getHeaders().get("_type");
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
                // Note: in the view case, calling withType instead of forType for compatibility with Jackson <2.5
                try {
                    if (payload instanceof byte[]) {
                        if (view != null) {
                            return getObjectMapper().readerWithView(view).forType(javaType).readValue((byte[]) payload);
                        } else {
                            return getObjectMapper().readValue((byte[]) payload, javaType);
                        }
                    } else {
                        if (view != null) {
                            return getObjectMapper().readerWithView(view).forType(javaType).readValue(payload.toString());
                        } else {
                            return getObjectMapper().readValue(payload.toString(), javaType);
                        }
                    }
                } catch (IOException ex) {
                    throw new MessageConversionException(message, "Could not read JSON: " + ex.getMessage(), ex);
                }
            }

        };
        converter.setObjectMapper(aObjectMapper);

        return converter;
    }

    @Bean
    MessageHandlerMethodFactory messageHandlerMethodFactory(MessageConverter aMessageConverter) {
        DefaultMessageHandlerMethodFactory messageHandlerMethodFactory = new DefaultMessageHandlerMethodFactory();
        messageHandlerMethodFactory.setMessageConverter(aMessageConverter);

        return messageHandlerMethodFactory;
    }

    @Override
    public void configureKafkaListeners(KafkaListenerEndpointRegistrar aRegistrar) {
        CoordinatorProperties coordinatorProperties = properties.getCoordinator();
        WorkerProperties workerProperties = properties.getWorker();

        if (coordinatorProperties.isEnabled()) {
            registerListenerEndpoint(aRegistrar, Queues.COMPLETIONS, coordinatorProperties.getSubscriptions().getCompletions(), coordinator, "complete");
            registerListenerEndpoint(aRegistrar, Queues.ERRORS, coordinatorProperties.getSubscriptions().getErrors(), coordinator, "handleError");
            registerListenerEndpoint(aRegistrar, Queues.EVENTS, coordinatorProperties.getSubscriptions().getEvents(), eventListener, "onApplicationEvent");
            registerListenerEndpoint(aRegistrar, Queues.JOBS, coordinatorProperties.getSubscriptions().getJobs(), coordinator, "start");
        }
        if (workerProperties.isEnabled()) {
            Map<String, Object> subscriptions = workerProperties.getSubscriptions();
            subscriptions.forEach((k, v) -> registerListenerEndpoint(aRegistrar, k, Integer.valueOf((String) v), worker, "handle"));
            registerListenerEndpoint(aRegistrar, Exchanges.CONTROL, 1, worker, "handle");
        }
    }

    private void registerListenerEndpoint(KafkaListenerEndpointRegistrar aRegistrar, String aQueueName, int aConcurrency, Object aDelegate, String aMethodName) {
        logger.info("Registring KAFKA Listener: {} -> {}:{}", aQueueName, aDelegate.getClass().getName(), aMethodName);

        Method listenerMethod = Stream.of(aDelegate.getClass().getMethods()).filter(it -> aMethodName.equals(it.getName())).findFirst().orElseThrow(() -> new IllegalArgumentException("No method found: " + aMethodName + " on " + aDelegate.getClass()));

        final MethodKafkaListenerEndpoint<String, String> endpoint = createListenerEndpoint(aQueueName, aDelegate, listenerMethod);

        aRegistrar.registerEndpoint(endpoint);
    }

    private MethodKafkaListenerEndpoint<String, String> createListenerEndpoint(String aQueueName, Object aListener, Method listenerMethod) {
        final MethodKafkaListenerEndpoint<String, String> endpoint = new MethodKafkaListenerEndpoint<>();

        endpoint.setBeanFactory(beanFactory);
        endpoint.setBean(aListener);
        endpoint.setMethod(listenerMethod);
        endpoint.setId(aQueueName + "Endpoint");
        endpoint.setTopics(aQueueName);
        endpoint.setMessageHandlerMethodFactory(messageHandlerMethodFactory);

        return endpoint;
    }

    private KafkaListenerContainerFactory createContainerFactory(int aConcurrency) {
        ConcurrentKafkaListenerContainerFactory factory = new ConcurrentKafkaListenerContainerFactory();
        factory.setConcurrency(aConcurrency);

        return factory;
    }


}
