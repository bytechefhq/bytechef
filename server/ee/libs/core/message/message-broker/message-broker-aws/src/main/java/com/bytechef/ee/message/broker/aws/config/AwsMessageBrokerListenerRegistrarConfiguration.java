/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.message.broker.aws.config;

import com.bytechef.message.broker.annotation.ConditionalOnMessageBrokerAws;
import com.bytechef.message.broker.config.MessageBrokerConfigurer;
import com.bytechef.message.broker.config.MessageBrokerListenerRegistrar;
import com.bytechef.message.route.MessageRoute;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.awspring.cloud.sqs.config.EndpointRegistrar;
import io.awspring.cloud.sqs.config.SqsEndpoint;
import io.awspring.cloud.sqs.config.SqsListenerConfigurer;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnMessageBrokerAws
public class AwsMessageBrokerListenerRegistrarConfiguration
    implements SqsListenerConfigurer, MessageBrokerListenerRegistrar<EndpointRegistrar> {

    private static final Logger log = LoggerFactory.getLogger(AwsMessageBrokerListenerRegistrarConfiguration.class);

    private final List<MessageBrokerConfigurer<EndpointRegistrar>> messageBrokerConfigurers;
    private final MessageHandlerMethodFactory messageHandlerMethodFactory;
    private final SqsMessageListenerContainerFactory<?> sqsMessageListenerContainerFactory;
    private final List<SqsMessageListenerContainer<?>> containers = new ArrayList<>();
    private final Map<String, ControlRouteListener> controlRouteListeners = new HashMap<>();
    private final AtomicInteger endpointSequence = new AtomicInteger();

    @SuppressFBWarnings("EI")
    public AwsMessageBrokerListenerRegistrarConfiguration(
        @Autowired(required = false) List<MessageBrokerConfigurer<EndpointRegistrar>> messageBrokerConfigurers,
        MessageHandlerMethodFactory messageHandlerMethodFactory,
        @Autowired(required = false) SqsMessageListenerContainerFactory<?> sqsMessageListenerContainerFactory) {

        this.messageBrokerConfigurers = messageBrokerConfigurers == null ? List.of() : messageBrokerConfigurers;
        this.messageHandlerMethodFactory = messageHandlerMethodFactory;
        this.sqsMessageListenerContainerFactory = sqsMessageListenerContainerFactory;
    }

    @Override
    public void configure(EndpointRegistrar listenerEndpointRegistrar) {
        for (MessageBrokerConfigurer<EndpointRegistrar> messageBrokerConfigurer : messageBrokerConfigurers) {
            messageBrokerConfigurer.configure(listenerEndpointRegistrar, this);
        }
    }

    @Override
    public void registerListenerEndpoint(
        EndpointRegistrar endpointRegistrar, MessageRoute messageRoute, int concurrency,
        Object delegate, String methodName) {

        Class<?> delegateClass = delegate.getClass();

        Method listenerMethod = Stream.of(delegateClass.getMethods())
            .filter(it -> methodName.equals(it.getName()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("No method found: " + methodName + " on " + delegate.getClass()));

        if (log.isTraceEnabled()) {
            log.trace("Registering AWS Listener: {} -> {}:{}", messageRoute, delegateClass.getName(), methodName);
        }

        if (messageRoute.isControlExchange()) {
            registerControlListenerEndpoint(endpointRegistrar, messageRoute, delegate, listenerMethod);

            return;
        }

        SqsEndpoint endpoint = createListenerEndpoint(messageRoute.getName(), delegate, listenerMethod);

        endpointRegistrar.registerEndpoint(endpoint);
    }

    private void registerControlListenerEndpoint(
        EndpointRegistrar endpointRegistrar, MessageRoute messageRoute, Object delegate, Method listenerMethod) {

        ControlRouteListener controlRouteListener = controlRouteListeners.get(messageRoute.getName());

        if (controlRouteListener == null) {
            controlRouteListener = new ControlRouteListener();

            controlRouteListeners.put(messageRoute.getName(), controlRouteListener);

            SqsEndpoint endpoint = createListenerEndpoint(
                messageRoute.getName(), controlRouteListener, ControlRouteListener.getReceiveMethod());

            endpointRegistrar.registerEndpoint(endpoint);
        }

        controlRouteListener.addInvocableHandlerMethod(
            messageHandlerMethodFactory.createInvocableHandlerMethod(delegate, listenerMethod));
    }

    @Override
    public void stopListenerEndpoints() {
        for (SqsMessageListenerContainer<?> container : containers) {
            try {
                container.stop();
            } catch (Exception e) {
                log.warn("Failed to stop AWS SQS listener container: {}", e.getMessage());
            }
        }
    }

    @Override
    public void startListenerEndpoints() {
        for (SqsMessageListenerContainer<?> container : containers) {
            try {
                container.start();
            } catch (Exception e) {
                log.warn("Failed to start AWS SQS listener container: {}", e.getMessage());
            }
        }
    }

    private SqsEndpoint createListenerEndpoint(String queueName, Object listener, Method listenerMethod) {
        queueName = queueName.replace(".", "-");

        SqsEndpoint endpoint = new SqsEndpoint.SqsEndpointBuilder()
            .id(queueName + "Endpoint" + endpointSequence.incrementAndGet())
            .queueNames(List.of(queueName))
            .acknowledgementMode(AcknowledgementMode.ON_SUCCESS)
            .build();

        endpoint.setBean(listener);
        endpoint.setMethod(listenerMethod);
        endpoint.setHandlerMethodFactory(messageHandlerMethodFactory);

        SqsMessageListenerContainer<?> container = sqsMessageListenerContainerFactory.createContainer(endpoint);
        containers.add(container);

        return endpoint;
    }

    public static final class ControlRouteListener {

        private final List<InvocableHandlerMethod> invocableHandlerMethods = new ArrayList<>();

        public void receive(Message<?> message) throws Exception {
            for (InvocableHandlerMethod invocableHandlerMethod : invocableHandlerMethods) {
                invocableHandlerMethod.invoke(message);
            }
        }

        private void addInvocableHandlerMethod(InvocableHandlerMethod invocableHandlerMethod) {
            invocableHandlerMethods.add(invocableHandlerMethod);
        }

        private static Method getReceiveMethod() {
            try {
                return ControlRouteListener.class.getMethod("receive", Message.class);
            } catch (NoSuchMethodException exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}
