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

package com.bytechef;

import com.bytechef.atlas.MapObject;
import com.bytechef.atlas.context.repository.ContextRepository;
import com.bytechef.atlas.context.repository.jdbc.JdbcContextRepository;
import com.bytechef.atlas.coordinator.event.EventListener;
import com.bytechef.atlas.coordinator.event.EventListenerChain;
import com.bytechef.atlas.counter.repository.CounterRepository;
import com.bytechef.atlas.counter.repository.jdbc.JdbcCounterRepository;
import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.job.repository.JobRepository;
import com.bytechef.atlas.job.repository.jdbc.JdbcJobRepository;
import com.bytechef.atlas.task.execution.repository.TaskExecutionRepository;
import com.bytechef.atlas.task.execution.repository.jdbc.JdbcTaskExecutionRepository;
import com.bytechef.atlas.workflow.repository.WorkflowRepository;
import com.bytechef.atlas.workflow.repository.mapper.JSONWorkflowMapper;
import com.bytechef.atlas.workflow.repository.mapper.WorkflowMapper;
import com.bytechef.atlas.workflow.repository.mapper.WorkflowMapperChain;
import com.bytechef.atlas.workflow.repository.mapper.YAMLWorkflowMapper;
import com.bytechef.atlas.workflow.repository.resource.ResourceBasedWorkflowRepository;
import com.bytechef.hermes.auth.jdbc.JdbcAuthenticationRepository;
import com.bytechef.hermes.auth.repository.AuthenticationRepository;
import com.bytechef.hermes.encryption.Encryption;
import com.bytechef.hermes.encryption.EncryptionKey;
import com.bytechef.hermes.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.hermes.file.storage.converter.FileEntryConverter;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * @author Ivica Cardic
 */
@ComponentScan
@EnableAutoConfiguration
@SpringBootConfiguration
public class IntTestConfiguration {

    @PostConstruct
    void afterPropertiesSet() {
        MapObject.addConverter(new FileEntryConverter());
    }

    @Bean
    AuthenticationRepository authenticationRepository(
            Encryption encryption, NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        JdbcAuthenticationRepository jdbcAuthenticationRepository = new JdbcAuthenticationRepository();

        jdbcAuthenticationRepository.setEncryption(encryption);
        jdbcAuthenticationRepository.setJdbcTemplate(jdbcTemplate);
        jdbcAuthenticationRepository.setObjectMapper(objectMapper);

        return jdbcAuthenticationRepository;
    }

    @Bean
    EncryptionKey encryptionKey() {
        return () -> "tTB1/UBIbYLuCXVi4PPfzA==";
    }

    @Bean
    FileStorageService fileStorageService() {
        return new Base64FileStorageService();
    }

    @Bean
    @Primary
    EventListenerChain eventListener(List<EventListener> eventListeners) {
        return new EventListenerChain(eventListeners);
    }

    @Bean
    EventPublisher eventPublisher(EventListener eventListener) {
        return eventListener::onApplicationEvent;
    }

    @Bean
    CounterRepository jdbcCounterRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcCounterRepository(jdbcTemplate);
    }

    @Bean
    TaskExecutionRepository jdbcJobTaskExecutionRepository(
            NamedParameterJdbcTemplate namedParameterJdbcTemplate, ObjectMapper objectMapper) {
        JdbcTaskExecutionRepository jdbcJobTaskRepository = new JdbcTaskExecutionRepository();

        jdbcJobTaskRepository.setJdbcTemplate(namedParameterJdbcTemplate);
        jdbcJobTaskRepository.setObjectMapper(objectMapper);

        return jdbcJobTaskRepository;
    }

    @Bean
    ContextRepository jdbcContextRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        JdbcContextRepository jdbcContextRepository = new JdbcContextRepository();

        jdbcContextRepository.setJdbcTemplate(jdbcTemplate);
        jdbcContextRepository.setObjectMapper(objectMapper);

        return jdbcContextRepository;
    }

    @Bean
    JobRepository jdbcJobRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate, ObjectMapper objectMapper) {
        JdbcJobRepository jdbcJobRepository = new JdbcJobRepository();

        jdbcJobRepository.setJdbcTemplate(namedParameterJdbcTemplate);
        jdbcJobRepository.setObjectMapper(objectMapper);

        return jdbcJobRepository;
    }

    @Bean
    WorkflowMapper workflowMapper() {
        return new WorkflowMapperChain(List.of(new JSONWorkflowMapper(), new YAMLWorkflowMapper()));
    }

    @Bean
    WorkflowRepository workflowRepository(WorkflowMapper workflowMapper) {
        return new ResourceBasedWorkflowRepository(workflowMapper);
    }
}
