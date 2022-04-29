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

package com.integri.atlas;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.engine.coordinator.event.ContextService;
import com.integri.atlas.engine.coordinator.event.EventListener;
import com.integri.atlas.engine.coordinator.event.EventListenerChain;
import com.integri.atlas.engine.coordinator.job.repository.JobRepository;
import com.integri.atlas.engine.core.MapObject;
import com.integri.atlas.engine.core.context.repository.ContextRepository;
import com.integri.atlas.engine.core.counter.repository.CounterRepository;
import com.integri.atlas.engine.core.event.EventPublisher;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;
import com.integri.atlas.engine.repository.jdbc.context.JdbcContextRepository;
import com.integri.atlas.engine.repository.jdbc.counter.JdbcCounterRepository;
import com.integri.atlas.engine.repository.jdbc.job.JdbcJobRepository;
import com.integri.atlas.engine.repository.jdbc.task.JdbcTaskExecutionRepository;
import com.integri.atlas.file.storage.FileStorageService;
import com.integri.atlas.file.storage.base64.Base64FileStorageService;
import com.integri.atlas.file.storage.converter.FileEntryConverter;
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
    ContextService contextService(
        ContextRepository contextRepository,
        TaskExecutionRepository taskExecutionRepository
    ) {
        return new ContextService(contextRepository, taskExecutionRepository);
    }

    @Bean
    CounterRepository counterRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcCounterRepository(jdbcTemplate);
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
    FileStorageService fileStorageService() {
        return new Base64FileStorageService();
    }

    @Bean
    TaskExecutionRepository jdbcJobTaskRepository(
        NamedParameterJdbcTemplate namedParameterJdbcTemplate,
        ObjectMapper objectMapper
    ) {
        JdbcTaskExecutionRepository jdbcJobTaskRepository = new JdbcTaskExecutionRepository();

        jdbcJobTaskRepository.setJdbcOperations(namedParameterJdbcTemplate);
        jdbcJobTaskRepository.setObjectMapper(objectMapper);

        return jdbcJobTaskRepository;
    }

    @Bean
    JobRepository jdbcJobRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate, ObjectMapper objectMapper) {
        JdbcJobRepository jdbcJobRepository = new JdbcJobRepository();

        jdbcJobRepository.setJdbcOperations(namedParameterJdbcTemplate);
        jdbcJobRepository.setJobTaskRepository(jdbcJobTaskRepository(namedParameterJdbcTemplate, objectMapper));
        jdbcJobRepository.setObjectMapper(objectMapper);

        return jdbcJobRepository;
    }

    @Bean
    ContextRepository jdbcContextRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        JdbcContextRepository jdbcContextRepository = new JdbcContextRepository();

        jdbcContextRepository.setJdbcTemplate(jdbcTemplate);
        jdbcContextRepository.setObjectMapper(objectMapper);

        return jdbcContextRepository;
    }
}
