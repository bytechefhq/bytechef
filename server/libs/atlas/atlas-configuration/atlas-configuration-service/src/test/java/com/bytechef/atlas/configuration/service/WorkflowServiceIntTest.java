/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.atlas.configuration.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.atlas.configuration.repository.WorkflowRepository;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@EnableCaching
public class WorkflowServiceIntTest {

    @Autowired
    private WorkflowCrudRepository workflowCrudRepository;

    @Autowired
    private WorkflowService workflowService;

    @Test
    public void testCreate() {
        Workflow workflow = getWorkflow();

        workflow = workflowService.create(
            workflow.getDefinition(), workflow.getFormat(), Workflow.SourceType.JDBC, 0);

        Assertions.assertEquals(workflow, OptionalUtils.get(workflowCrudRepository.findById(workflow.getId())));
    }

    @Test
    public void testDelete() {
        Workflow workflow = workflowCrudRepository.save(getWorkflow());

        workflowService.delete(Validate.notNull(workflow.getId(), "id"));

        Assertions.assertFalse(OptionalUtils.isPresent(workflowCrudRepository.findById(workflow.getId())));
    }

    @Test
    public void testGetWorkflow() {
        Workflow workflow = workflowCrudRepository.save(getWorkflow());

        Assertions.assertEquals(workflow, workflowService.getWorkflow(Validate.notNull(workflow.getId(), "id")));
    }

    @Test
    public void testGetWorkflows() {
        for (Workflow workflow : workflowCrudRepository.findAll(0)) {
            workflowCrudRepository.deleteById(workflow.getId());
        }

        workflowCrudRepository.save(getWorkflow());

        Assertions.assertEquals(1, CollectionUtils.size(workflowService.getWorkflows(0)));
    }

    @Test
    public void testUpdate() {
        String definition = "{\"label\": \"Label\",\"tasks\": []}";
        Workflow workflow = workflowCrudRepository.save(getWorkflow());

        Workflow updatedWorkflow = workflowService.update(
            Validate.notNull(workflow.getId(), "id"), definition, workflow.getVersion());

        Assertions.assertEquals(definition, updatedWorkflow.getDefinition());
    }

    private static Workflow getWorkflow() {
        Workflow workflow = new Workflow("{\"tasks\": []}", Workflow.Format.JSON, 0);

        workflow.setNew(true);

        return workflow;
    }

    @ComponentScan(
        basePackages = {
            "com.bytechef.atlas.configuration.repository.jdbc",
            "com.bytechef.liquibase.config"
        })
    @EnableAutoConfiguration
    @Configuration
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public static class WorkflowConfigurationIntTestConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        MapUtils mapUtils() {
            return new MapUtils() {
                {
                    objectMapper = objectMapper();
                }
            };
        }

        @Bean
        WorkflowService workflowService(
            CacheManager cacheManager, List<WorkflowCrudRepository> workflowCrudRepositories,
            List<WorkflowRepository> workflowRepositories) {

            return new WorkflowServiceImpl(cacheManager, workflowCrudRepositories, workflowRepositories);
        }

        @EnableJdbcRepositories(basePackages = "com.bytechef.atlas.configuration.repository.jdbc")
        public static class WorkflowIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
        }
    }
}
