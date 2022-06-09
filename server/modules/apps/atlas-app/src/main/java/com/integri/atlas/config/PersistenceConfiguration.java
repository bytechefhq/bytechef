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
import com.integri.atlas.engine.workflow.repository.WorkflowRepositoryChain;
import com.integri.atlas.engine.workflow.repository.git.GitWorkflowRepository;
import com.integri.atlas.engine.workflow.repository.jdbc.JdbcWorkflowRepository;
import com.integri.atlas.engine.workflow.repository.mapper.JsonWorkflowMapper;
import com.integri.atlas.engine.workflow.repository.mapper.WorkflowMapper;
import com.integri.atlas.engine.workflow.repository.mapper.WorkflowMapperChain;
import com.integri.atlas.engine.workflow.repository.mapper.YAMLWorkflowMapper;
import com.integri.atlas.engine.workflow.repository.resource.ResourceBasedWorkflowRepository;
import com.integri.atlas.task.auth.jdbc.JdbcTaskAuthRepository;
import com.integri.atlas.task.auth.repository.TaskAuthRepository;
import java.util.List;
import com.integri.atlas.task.definition.repository.ExtTaskDefinitionHandlerRepository;
import com.integri.atlas.task.definition.repository.memory.InMemoryExtTaskDefinitionHandlerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@Configuration
public class PersistenceConfiguration {

    @Bean
    ExtTaskDefinitionHandlerRepository extTaskDefinitionHandlerRepository() {
        return new InMemoryExtTaskDefinitionHandlerRepository();
    }

    @ConditionalOnProperty(name = "atlas.persistence.provider", havingValue = "jdbc")
    public static class JdbcRepositoryConfiguration {

        @Bean
        CounterRepository counterRepository(JdbcTemplate jdbcTemplate) {
            return new JdbcCounterRepository(jdbcTemplate);
        }

        @Bean
        ContextRepository contextRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
            JdbcContextRepository jdbcContextRepository = new JdbcContextRepository();

            jdbcContextRepository.setJdbcTemplate(jdbcTemplate);
            jdbcContextRepository.setObjectMapper(objectMapper);

            return jdbcContextRepository;
        }

        @Bean
        JobRepository jobRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
            JdbcJobRepository jdbcJobRepository = new JdbcJobRepository();

            jdbcJobRepository.setJdbcTemplate(jdbcTemplate);
            jdbcJobRepository.setObjectMapper(objectMapper);

            return jdbcJobRepository;
        }

        @Bean
        TaskExecutionRepository taskExecutionRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
        ) {
            JdbcTaskExecutionRepository jdbcTaskExecutionRepository = new JdbcTaskExecutionRepository();

            jdbcTaskExecutionRepository.setJdbcTemplate(jdbcTemplate);
            jdbcTaskExecutionRepository.setObjectMapper(objectMapper);

            return jdbcTaskExecutionRepository;
        }

        @Bean
        TaskAuthRepository taskAuthRepository(
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
    }

    @EnableConfigurationProperties(AtlasProperties.class)
    public static class WorkflowRepositoryConfiguration {

        @Bean
        @Primary
        WorkflowRepository workflowRepository(
            CacheManager cacheManager,
            List<WorkflowRepository> workflowRepositories
        ) {
            return new WorkflowRepositoryChain(cacheManager, workflowRepositories);
        }

        @Bean
        @Order(1)
        @ConditionalOnProperty(name = "atlas.workflow-repository.classpath.enabled", havingValue = "true")
        ResourceBasedWorkflowRepository resourceBasedWorkflowRepository() {
            return new ResourceBasedWorkflowRepository(workflowMapper());
        }

        @Bean
        @Order(2)
        @ConditionalOnProperty(name = "atlas.workflow-repository.filesystem.enabled", havingValue = "true")
        ResourceBasedWorkflowRepository fileSystemBasedWorkflowRepository(
            @Value("${atlas.workflow-repository.filesystem.location-pattern}") String aBasePath
        ) {
            return new ResourceBasedWorkflowRepository(String.format("file:%s", aBasePath), workflowMapper());
        }

        @Bean
        @Order(3)
        @ConditionalOnProperty(name = "atlas.workflow-repository.git.enabled", havingValue = "true")
        GitWorkflowRepository gitWorkflowRepository(AtlasProperties atlasProperties) {
            WorkflowRepositoryProperties.GitProperties gitProperties = atlasProperties.getWorkflowRepository().getGit();

            return new GitWorkflowRepository(
                gitProperties.getUrl(),
                gitProperties.getBranch(),
                gitProperties.getSearchPaths(),
                gitProperties.getUsername(),
                gitProperties.getPassword(),
                workflowMapper()
            );
        }

        @Bean
        @ConditionalOnProperty(name = "atlas.workflow-repository.database.enabled", havingValue = "true")
        @Order(4)
        JdbcWorkflowRepository jdbcWorkflowRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            WorkflowMapper aWorkflowMapper
        ) {
            JdbcWorkflowRepository jdbcWorkflowRepository = new JdbcWorkflowRepository();

            jdbcWorkflowRepository.setJdbcTemplate(jdbcTemplate);
            jdbcWorkflowRepository.setWorkflowMapper(aWorkflowMapper);

            return jdbcWorkflowRepository;
        }

        @Bean
        @Primary
        WorkflowMapper workflowMapper() {
            return new WorkflowMapperChain(List.of(jsonWorkflowMapper(), yamlWorkflowMapper()));
        }

        @Bean
        JsonWorkflowMapper jsonWorkflowMapper() {
            return new JsonWorkflowMapper();
        }

        @Bean
        YAMLWorkflowMapper yamlWorkflowMapper() {
            return new YAMLWorkflowMapper();
        }
    }
}
