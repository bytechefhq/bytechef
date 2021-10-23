/*
 * Copyright (C) 2021 <your company/name>.
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

package com.integri.atlas.engine.coordinator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.repository.engine.jdbc.context.MysqlJdbcContextRepository;
import com.integri.atlas.repository.engine.jdbc.context.PostgresJdbcContextRepository;
import com.integri.atlas.engine.coordinator.job.repository.JobRepository;
import com.integri.atlas.repository.engine.jdbc.job.MysqlJdbcJobRepository;
import com.integri.atlas.repository.engine.jdbc.job.PostgresJdbcJobRepository;
import com.integri.atlas.repository.engine.jdbc.task.MysqlJdbcCounterRepository;
import com.integri.atlas.repository.engine.jdbc.task.MysqlJdbcTaskExecutionRepository;
import com.integri.atlas.repository.engine.jdbc.task.PostgresJdbcCounterRepository;
import com.integri.atlas.repository.engine.jdbc.task.PostgresJdbcTaskExecutionRepository;
import com.integri.atlas.engine.core.context.repository.ContextRepository;
import com.integri.atlas.engine.core.task.repository.CounterRepository;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * @author Ivica Cardic
 */
@EnableAutoConfiguration
@ImportAutoConfiguration(DataSourceAutoConfiguration.class)
@SpringBootConfiguration
public class TestConfiguration {

    @Configuration
    @ConditionalOnProperty(name = "spring.sql.init.platform", havingValue = "mysql")
    public static class MysqlJdbcPersistenceConfiguration {

        @Bean
        TaskExecutionRepository jdbcJobTaskRepository(
            NamedParameterJdbcTemplate aJdbcTemplate,
            ObjectMapper aObjectMapper
        ) {
            MysqlJdbcTaskExecutionRepository jdbcJobTaskRepository = new MysqlJdbcTaskExecutionRepository();
            jdbcJobTaskRepository.setJdbcOperations(aJdbcTemplate);
            jdbcJobTaskRepository.setObjectMapper(aObjectMapper);
            return jdbcJobTaskRepository;
        }

        @Bean
        JobRepository jdbcJobRepository(NamedParameterJdbcTemplate aJdbcTemplate, ObjectMapper aObjectMapper) {
            MysqlJdbcJobRepository jdbcJobRepository = new MysqlJdbcJobRepository();
            jdbcJobRepository.setJdbcOperations(aJdbcTemplate);
            jdbcJobRepository.setJobTaskRepository(jdbcJobTaskRepository(aJdbcTemplate, aObjectMapper));
            return jdbcJobRepository;
        }

        @Bean
        ContextRepository jdbcContextRepository(JdbcTemplate aJdbcTemplate, ObjectMapper aObjectMapper) {
            MysqlJdbcContextRepository repo = new MysqlJdbcContextRepository();
            repo.setJdbcTemplate(aJdbcTemplate);
            repo.setObjectMapper(aObjectMapper);
            return repo;
        }

        @Bean
        CounterRepository counterRepository(JdbcTemplate aJdbcOperations) {
            return new MysqlJdbcCounterRepository(aJdbcOperations);
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "spring.sql.init.platform", havingValue = "postgres")
    public static class PostgresJdbcPersistenceConfiguration {

        @Bean
        TaskExecutionRepository jdbcJobTaskRepository(NamedParameterJdbcTemplate aJdbcTemplate, ObjectMapper aObjectMapper) {
            PostgresJdbcTaskExecutionRepository jdbcJobTaskRepository = new PostgresJdbcTaskExecutionRepository();
            jdbcJobTaskRepository.setJdbcOperations(aJdbcTemplate);
            jdbcJobTaskRepository.setObjectMapper(aObjectMapper);
            return jdbcJobTaskRepository;
        }

        @Bean
        JobRepository jdbcJobRepository(NamedParameterJdbcTemplate aJdbcTemplate, ObjectMapper aObjectMapper) {
            PostgresJdbcJobRepository jdbcJobRepository = new PostgresJdbcJobRepository();
            jdbcJobRepository.setJdbcOperations(aJdbcTemplate);
            jdbcJobRepository.setJobTaskRepository(jdbcJobTaskRepository(aJdbcTemplate, aObjectMapper));
            return jdbcJobRepository;
        }

        @Bean
        ContextRepository jdbcContextRepository(JdbcTemplate aJdbcTemplate, ObjectMapper aObjectMapper) {
            PostgresJdbcContextRepository repo = new PostgresJdbcContextRepository();
            repo.setJdbcTemplate(aJdbcTemplate);
            repo.setObjectMapper(aObjectMapper);
            return repo;
        }

        @Bean
        CounterRepository counterRepository(JdbcTemplate aJdbcOperations) {
            return new PostgresJdbcCounterRepository(aJdbcOperations);
        }
    }
}
