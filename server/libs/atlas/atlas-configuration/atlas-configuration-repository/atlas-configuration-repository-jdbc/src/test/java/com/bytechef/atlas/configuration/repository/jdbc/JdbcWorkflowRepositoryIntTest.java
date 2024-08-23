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

package com.bytechef.atlas.configuration.repository.jdbc;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.WorkflowCrudRepository;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
public class JdbcWorkflowRepositoryIntTest {

    @Autowired
    private WorkflowCrudRepository workflowCrudRepository;

    @Test
    public void testFindById() {
        Workflow workflow = new Workflow("""
            label: My Label

            tasks:
              - name: stringNumber
                type: var/1.0/set
                parameters:
                  value: "1234"
            """,
            Workflow.Format.YAML);

        workflow.setNew(true);

        workflow = workflowCrudRepository.save(workflow);

        Workflow resultWorkflow = OptionalUtils.get(workflowCrudRepository.findById(workflow.getId()));

        Assertions.assertEquals("My Label", resultWorkflow.getLabel());
        Assertions.assertEquals(1, CollectionUtils.size(resultWorkflow.getTasks()));
    }

    @ComponentScan(
        basePackages = {
            "com.bytechef.atlas.configuration.repository.jdbc",
        })
    @EnableAutoConfiguration
    @EnableCaching
    @Configuration
    @Import(LiquibaseConfiguration.class)
    public static class WorkflowConfigurationRepositoryIntTestConfiguration {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module());
        }

        @EnableJdbcRepositories(basePackages = "com.bytechef.atlas.configuration.repository.jdbc")
        public static class WorkflowConfigurationIntJdbcTestConfiguration extends AbstractIntTestJdbcConfiguration {

            private final ObjectMapper objectMapper;

            @SuppressFBWarnings("EI2")
            public WorkflowConfigurationIntJdbcTestConfiguration(ObjectMapper objectMapper) {
                this.objectMapper = objectMapper;
            }

            @Override
            protected List<?> userConverters() {
                return Arrays.asList(
                    new MapWrapperToStringConverter(objectMapper),
                    new StringToMapWrapperConverter(objectMapper),
                    new StringToWorkflowTaskConverter(objectMapper),
                    new WorkflowTaskToStringConverter(objectMapper));
            }
        }
    }
}
