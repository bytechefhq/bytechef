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

package com.integri.atlas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.encryption.Encryption;
import com.integri.atlas.engine.context.repository.ContextRepository;
import com.integri.atlas.engine.context.repository.jdbc.JdbcContextRepository;
import com.integri.atlas.engine.counter.repository.CounterRepository;
import com.integri.atlas.engine.counter.repository.jdbc.JdbcCounterRepository;
import com.integri.atlas.engine.job.repository.JobRepository;
import com.integri.atlas.engine.job.repository.jdbc.JdbcJobRepository;
import com.integri.atlas.engine.task.execution.repository.TaskExecutionRepository;
import com.integri.atlas.engine.task.execution.repository.jdbc.JdbcTaskExecutionRepository;
import com.integri.atlas.engine.workflow.repository.WorkflowRepository;
import com.integri.atlas.engine.workflow.repository.jdbc.JdbcWorkflowRepository;
import com.integri.atlas.engine.workflow.repository.mapper.WorkflowMapper;
import com.integri.atlas.task.auth.JdbcTaskAuthRepository;
import com.integri.atlas.task.auth.repository.TaskAuthRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * @author Arik Cohen
 */
@Configuration
@ConditionalOnProperty(name = "atlas.persistence.provider", havingValue = "jdbc")
public class JdbcPersistenceConfiguration {

    @Bean
    CounterRepository jdbcCounterRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcCounterRepository(jdbcTemplate);
    }

    @Bean
    ContextRepository jdbcContextRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        JdbcContextRepository jdbcContextRepository = new JdbcContextRepository();

        jdbcContextRepository.setJdbcTemplate(jdbcTemplate);
        jdbcContextRepository.setObjectMapper(objectMapper);

        return jdbcContextRepository;
    }

    @Bean
    JobRepository jdbcJobRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        JdbcJobRepository jdbcJobRepository = new JdbcJobRepository();

        jdbcJobRepository.setJdbcTemplate(jdbcTemplate);
        jdbcJobRepository.setObjectMapper(objectMapper);

        return jdbcJobRepository;
    }

    @Bean
    TaskExecutionRepository jdbcTaskExecutionRepository(
        NamedParameterJdbcTemplate jdbcTemplate,
        ObjectMapper objectMapper
    ) {
        JdbcTaskExecutionRepository jdbcTaskExecutionRepository = new JdbcTaskExecutionRepository();

        jdbcTaskExecutionRepository.setJdbcTemplate(jdbcTemplate);
        jdbcTaskExecutionRepository.setObjectMapper(objectMapper);

        return jdbcTaskExecutionRepository;
    }

    @Bean
    TaskAuthRepository jdbcTaskAuthRepository(
        Encryption encryption,
        NamedParameterJdbcTemplate jdbcTemplate,
        ObjectMapper objectMapper
    ) {
        JdbcTaskAuthRepository jdbcTaskAuthRepository = new JdbcTaskAuthRepository();

        jdbcTaskAuthRepository.setEncryption(encryption);
        jdbcTaskAuthRepository.setJdbcTemplate(jdbcTemplate);
        jdbcTaskAuthRepository.setObjectMapper(objectMapper);

        return jdbcTaskAuthRepository;
    }

    @Bean
    @ConditionalOnProperty(name = "atlas.workflow-repository.database.enabled", havingValue = "true")
    @Order(4)
    WorkflowRepository jdbcWorkflowRepository(NamedParameterJdbcTemplate jdbcTemplate, WorkflowMapper aWorkflowMapper) {
        JdbcWorkflowRepository jdbcWorkflowRepository = new JdbcWorkflowRepository();

        jdbcWorkflowRepository.setJdbcTemplate(jdbcTemplate);
        jdbcWorkflowRepository.setWorkflowMapper(aWorkflowMapper);

        return jdbcWorkflowRepository;
    }
}
