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

package com.bytechef.platform.data.table.config;

import static org.mockito.Mockito.mock;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.message.event.MessageEventPreSendProcessor;
import com.bytechef.message.event.listener.MessageEventListener;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = "com.bytechef.platform.data.table")
@EnableAsync
@EnableAutoConfiguration
@EnableConfigurationProperties(ApplicationProperties.class)
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class, MessageEventListener.class
})
@Configuration
public class DataTableIntTestConfiguration {

    @Bean
    DisabledWorkflowJobPrincipalAccessor disabledWorkflowJobPrincipalAccessor() {
        return new DisabledWorkflowJobPrincipalAccessor();
    }

    @Bean
    JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry(
        DisabledWorkflowJobPrincipalAccessor disabledWorkflowJobPrincipalAccessor) {

        return new JobPrincipalAccessorRegistry(List.of(disabledWorkflowJobPrincipalAccessor));
    }

    @Bean
    RecordingMessageBroker messageBroker() {
        return new RecordingMessageBroker();
    }

    @Bean
    TaskExecutor messageEventExecutor() {
        return new SyncTaskExecutor();
    }

    @Bean
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    MessageEventPreSendProcessor passThroughMessageEventPreSendProcessor() {
        return messageEvent -> messageEvent;
    }

    @Bean
    TagService tagService() {
        return mock(TagService.class);
    }

    @Bean
    AsyncTaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor("task-");
    }

    @Bean
    AsyncTaskExecutor workerExecutor() {
        SimpleAsyncTaskExecutor simpleAsyncTaskExecutor = new SimpleAsyncTaskExecutor("worker-");

        simpleAsyncTaskExecutor.setConcurrencyLimit(1);

        return simpleAsyncTaskExecutor;
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class DataTableIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
