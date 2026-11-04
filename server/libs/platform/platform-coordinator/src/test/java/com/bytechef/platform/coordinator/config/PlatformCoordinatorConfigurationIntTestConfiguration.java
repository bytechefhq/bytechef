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

package com.bytechef.platform.coordinator.config;

import static org.mockito.Mockito.mock;

import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.JobServiceImpl;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.execution.service.TaskExecutionServiceImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.message.broker.MessageBroker;
import com.bytechef.platform.coordinator.config.PlatformCoordinatorIntTestConfiguration.PlatformCoordinatorIntTestJdbcConfiguration;
import com.bytechef.platform.notification.delivery.WebhookDeliveryService;
import com.bytechef.platform.notification.handler.NotificationHandlerRegistry;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.repository.NotificationRepository;
import com.bytechef.platform.notification.service.NotificationService;
import com.bytechef.platform.notification.service.NotificationServiceImpl;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@EnableAutoConfiguration
@EnableConfigurationProperties(ApplicationProperties.class)
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class, PlatformCoordinatorConfiguration.class,
    PlatformCoordinatorIntTestJdbcConfiguration.class, PostgreSQLContainerConfiguration.class
})
@Configuration
public class PlatformCoordinatorConfigurationIntTestConfiguration {

    @Bean
    JobService jobService(JobRepository jobRepository) {
        return new JobServiceImpl(jobRepository);
    }

    @Bean
    MessageBroker messageBroker() {
        return mock(MessageBroker.class);
    }

    @Bean
    NotificationHandlerRegistry notificationHandlerRegistry() {
        return new NotificationHandlerRegistry(List.of());
    }

    @Bean
    NotificationSenderRegistry notificationSenderRegistry() {
        return new NotificationSenderRegistry(List.of());
    }

    @Bean
    NotificationService notificationService(NotificationRepository notificationRepository) {
        return new NotificationServiceImpl(notificationRepository);
    }

    @Bean
    TaskExecutionService taskExecutionService(TaskExecutionRepository taskExecutionRepository) {
        return new TaskExecutionServiceImpl(taskExecutionRepository);
    }

    @Bean
    TaskFileStorage taskFileStorage() {
        return mock(TaskFileStorage.class);
    }

    @Bean
    TenantService tenantService() {
        return mock(TenantService.class);
    }

    @Bean
    WebhookDeliveryService webhookDeliveryService() {
        return mock(WebhookDeliveryService.class);
    }
}
