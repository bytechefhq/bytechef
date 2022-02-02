/*
 * Copyright 2021 <your company/name>.
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

package com.integri.atlas;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.engine.coordinator.job.repository.JobRepository;
import com.integri.atlas.engine.core.context.repository.ContextRepository;
import com.integri.atlas.engine.core.json.DefaultJSONHelper;
import com.integri.atlas.engine.core.json.JSONHelper;
import com.integri.atlas.engine.core.task.repository.CounterRepository;
import com.integri.atlas.engine.core.task.repository.TaskExecutionRepository;
import com.integri.atlas.engine.repository.jdbc.context.JdbcContextRepository;
import com.integri.atlas.engine.repository.jdbc.counter.JdbcCounterRepository;
import com.integri.atlas.engine.repository.jdbc.job.JdbcJobRepository;
import com.integri.atlas.engine.repository.jdbc.task.JdbcTaskExecutionRepository;
import com.integri.atlas.file.storage.FileStorageService;
import com.integri.atlas.file.storage.base64.Base64FileStorageService;
import com.integri.atlas.file.storage.converter.FileEntryConverter;
import javax.annotation.PostConstruct;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * @author Ivica Cardic
 */
@EnableAutoConfiguration
@ImportAutoConfiguration(DataSourceAutoConfiguration.class)
@SpringBootConfiguration
public class TestConfiguration {

    @Bean
    CounterRepository counterRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcCounterRepository(jdbcTemplate);
    }

    @Bean
    TaskExecutionRepository jdbcJobTaskRepository(
        NamedParameterJdbcTemplate namedParameterJdbcTemplate,
        JSONHelper jsonHelper
    ) {
        JdbcTaskExecutionRepository jdbcJobTaskRepository = new JdbcTaskExecutionRepository();

        jdbcJobTaskRepository.setJdbcOperations(namedParameterJdbcTemplate);
        jdbcJobTaskRepository.setJsonHelper(jsonHelper);

        return jdbcJobTaskRepository;
    }

    @Bean
    JobRepository jdbcJobRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate, JSONHelper jsonHelper) {
        JdbcJobRepository jdbcJobRepository = new JdbcJobRepository();

        jdbcJobRepository.setJdbcOperations(namedParameterJdbcTemplate);
        jdbcJobRepository.setJobTaskRepository(jdbcJobTaskRepository(namedParameterJdbcTemplate, jsonHelper));
        jdbcJobRepository.setJsonHelper(jsonHelper);

        return jdbcJobRepository;
    }

    @Bean
    ContextRepository jdbcContextRepository(JdbcTemplate jdbcTemplate, JSONHelper jsonHelper) {
        JdbcContextRepository jdbcContextRepository = new JdbcContextRepository();

        jdbcContextRepository.setJdbcTemplate(jdbcTemplate);
        jdbcContextRepository.setJsonHelper(jsonHelper);

        return jdbcContextRepository;
    }

    @Bean
    JSONHelper jsonMapper(ObjectMapper objectMapper) {
        return new DefaultJSONHelper(objectMapper);
    }

    @Bean
    FileStorageService fileStorageService() {
        return new Base64FileStorageService();
    }
}
