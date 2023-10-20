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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.engine.coordinator.context.JdbcContextRepository;
import com.integri.atlas.engine.coordinator.job.JdbcJobRepository;
import com.integri.atlas.engine.core.task.CounterRepository;
import com.integri.atlas.engine.coordinator.task.JdbcCounterRepository;
import com.integri.atlas.engine.coordinator.task.JdbcTaskExecutionRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
@ConditionalOnProperty(name = "atlas.persistence.provider", havingValue = "jdbc")
public class JdbcPersistenceConfiguration {

    @Bean
    JdbcTaskExecutionRepository jdbcJobTaskRepository(
        NamedParameterJdbcTemplate aJdbcTemplate,
        ObjectMapper aObjectMapper
    ) {
        JdbcTaskExecutionRepository jdbcJobTaskRepository = new JdbcTaskExecutionRepository();
        jdbcJobTaskRepository.setJdbcOperations(aJdbcTemplate);
        jdbcJobTaskRepository.setObjectMapper(aObjectMapper);
        return jdbcJobTaskRepository;
    }

    @Bean
    JdbcJobRepository jdbcJobRepository(NamedParameterJdbcTemplate aJdbcTemplate, ObjectMapper aObjectMapper) {
        JdbcJobRepository jdbcJobRepository = new JdbcJobRepository();
        jdbcJobRepository.setJdbcOperations(aJdbcTemplate);
        jdbcJobRepository.setJobTaskRepository(jdbcJobTaskRepository(aJdbcTemplate, aObjectMapper));
        return jdbcJobRepository;
    }

    @Bean
    JdbcContextRepository jdbcContextRepository(JdbcTemplate aJdbcTemplate, ObjectMapper aObjectMapper) {
        JdbcContextRepository repo = new JdbcContextRepository();
        repo.setJdbcTemplate(aJdbcTemplate);
        repo.setObjectMapper(aObjectMapper);
        return repo;
    }

    @Bean
    CounterRepository counterRepository(JdbcTemplate aJdbcOperations) {
        return new JdbcCounterRepository(aJdbcOperations);
    }
}
