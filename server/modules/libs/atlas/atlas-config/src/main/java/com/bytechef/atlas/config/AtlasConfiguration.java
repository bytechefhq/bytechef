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

package com.bytechef.atlas.config;

import com.bytechef.atlas.repository.context.ContextRepository;
import com.bytechef.atlas.repository.counter.CounterRepository;
import com.bytechef.atlas.repository.git.workflow.GitWorkflowRepository;
import com.bytechef.atlas.repository.jdbc.context.JdbcContextRepository;
import com.bytechef.atlas.repository.jdbc.counter.JdbcCounterRepository;
import com.bytechef.atlas.repository.jdbc.job.JdbcJobRepository;
import com.bytechef.atlas.repository.jdbc.task.execution.JdbcTaskExecutionRepository;
import com.bytechef.atlas.repository.jdbc.workflow.JdbcWorkflowRepository;
import com.bytechef.atlas.repository.job.JobRepository;
import com.bytechef.atlas.repository.resource.workflow.ResourceBasedWorkflowRepository;
import com.bytechef.atlas.repository.task.execution.TaskExecutionRepository;
import com.bytechef.atlas.repository.workflow.WorkflowRepository;
import com.bytechef.atlas.repository.workflow.WorkflowRepositoryChain;
import com.bytechef.atlas.repository.workflow.mapper.JsonWorkflowMapper;
import com.bytechef.atlas.repository.workflow.mapper.WorkflowMapper;
import com.bytechef.atlas.repository.workflow.mapper.WorkflowMapperChain;
import com.bytechef.atlas.repository.workflow.mapper.YamlWorkflowMapper;
import com.bytechef.atlas.service.context.ContextService;
import com.bytechef.atlas.service.counter.CounterService;
import com.bytechef.atlas.service.job.JobService;
import com.bytechef.atlas.service.task.execution.TaskExecutionService;
import com.bytechef.atlas.service.workflow.WorkflowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
public class AtlasConfiguration {

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
                NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
            JdbcTaskExecutionRepository jdbcTaskExecutionRepository = new JdbcTaskExecutionRepository();

            jdbcTaskExecutionRepository.setJdbcTemplate(jdbcTemplate);
            jdbcTaskExecutionRepository.setObjectMapper(objectMapper);

            return jdbcTaskExecutionRepository;
        }
    }

    @EnableConfigurationProperties(AtlasProperties.class)
    public static class WorkflowRepositoryConfiguration {

        @Bean
        @Primary
        WorkflowRepository workflowRepository(
                CacheManager cacheManager, List<WorkflowRepository> workflowRepositories) {
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
                @Value("${atlas.workflow-repository.filesystem.location-pattern}") String aBasePath) {
            return new ResourceBasedWorkflowRepository(String.format("file:%s", aBasePath), workflowMapper());
        }

        @Bean
        @Order(3)
        @ConditionalOnProperty(name = "atlas.workflow-repository.git.enabled", havingValue = "true")
        GitWorkflowRepository gitWorkflowRepository(AtlasProperties atlasProperties) {
            WorkflowRepositoryProperties.GitProperties gitProperties =
                    atlasProperties.getWorkflowRepository().getGit();

            return new GitWorkflowRepository(
                    gitProperties.getUrl(),
                    gitProperties.getBranch(),
                    gitProperties.getSearchPaths(),
                    gitProperties.getUsername(),
                    gitProperties.getPassword(),
                    workflowMapper());
        }

        @Bean
        @ConditionalOnProperty(name = "atlas.workflow-repository.database.enabled", havingValue = "true")
        @Order(4)
        JdbcWorkflowRepository jdbcWorkflowRepository(NamedParameterJdbcTemplate jdbcTemplate) {
            JdbcWorkflowRepository jdbcWorkflowRepository = new JdbcWorkflowRepository();

            jdbcWorkflowRepository.setJdbcTemplate(jdbcTemplate);
            jdbcWorkflowRepository.setWorkflowMapper(workflowMapper());

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
        YamlWorkflowMapper yamlWorkflowMapper() {
            return new YamlWorkflowMapper();
        }
    }

    @Bean
    public ContextService contextService(
            ContextRepository contextRepository, TaskExecutionRepository taskExecutionRepository) {
        return new ContextService(contextRepository, taskExecutionRepository);
    }

    @Bean
    public CounterService counterService(CounterRepository counterRepository) {
        return new CounterService(counterRepository);
    }

    @Bean
    public JobService jobService(
            JobRepository jobRepository,
            TaskExecutionRepository taskExecutionRepository,
            WorkflowRepository workflowRepository) {
        return new JobService(jobRepository, taskExecutionRepository, workflowRepository);
    }

    @Bean
    public TaskExecutionService taskExecutionService(TaskExecutionRepository taskExecutionRepository) {
        return new TaskExecutionService(taskExecutionRepository);
    }

    @Bean
    public WorkflowService workflowService(WorkflowRepository workflowRepository) {
        return new WorkflowService(workflowRepository);
    }
}
