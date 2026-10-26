/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.message.broker.MessageBroker;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.BindableService;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest
@Import({
    PostgreSQLContainerConfiguration.class
})
public class SchedulerApplicationIntTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Test
    void testContextLoads() {
    }

    @Test
    void testSharedAndAppConfigurationLoad() {
        assertThat(environment.getProperty("spring.task.execution.thread-name-prefix"))
            .isEqualTo("scheduler-app-task-");
        assertThat(environment.getProperty("docs.documentation-url")).isEqualTo("https://docs.bytechef.io");
    }

    @Test
    void testSchedulerGrpcServicesAreServed() {
        List<String> serviceNames = applicationContext.getBeansOfType(BindableService.class)
            .values()
            .stream()
            .map(bindableService -> bindableService.bindService()
                .getServiceDescriptor()
                .getName())
            .toList();

        assertThat(serviceNames).contains(
            "bytechef.platform.scheduler.remote.v1.TriggerSchedulerRemoteService",
            "bytechef.platform.scheduler.remote.v1.ConnectionRefreshSchedulerRemoteService");
    }

    @Nested
    @TestPropertySource(properties = "bytechef.message-broker.provider=amqp")
    class AmqpMessageBrokerProvider {

        @Autowired
        private MessageBroker messageBroker;

        @Test
        void testAmqpMessageBrokerIsAvailable() {
            Class<?> messageBrokerClass = messageBroker.getClass();

            assertThat(messageBrokerClass.getName()).isEqualTo("com.bytechef.message.broker.amqp.AmqpMessageBroker");
        }
    }
}
