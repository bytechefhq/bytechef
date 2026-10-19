/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.configuration.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.domain.StringProperty;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = WorkflowNodeTestOutputRepositoryIntTest.TestConfiguration.class)
class WorkflowNodeTestOutputRepositoryIntTest {

    @Autowired
    private WorkflowNodeTestOutputRepository workflowNodeTestOutputRepository;

    @AfterEach
    void afterEach() {
        workflowNodeTestOutputRepository.deleteAll();
    }

    @Test
    void testSaveKeepsSampleOutputTruncated() {
        WorkflowNodeTestOutput workflowNodeTestOutput = new WorkflowNodeTestOutput();

        workflowNodeTestOutput.setEnvironmentId(0L);
        workflowNodeTestOutput.setOutputSchema(new StringProperty(ComponentDsl.string("value")));
        workflowNodeTestOutput.setSampleOutput(List.of("first", "second"));
        workflowNodeTestOutput.setSampleOutputTruncated(true);
        workflowNodeTestOutput.setTypeName("oracle");
        workflowNodeTestOutput.setTypeOperationName("query");
        workflowNodeTestOutput.setTypeVersion(1);
        workflowNodeTestOutput.setWorkflowId("workflow1");
        workflowNodeTestOutput.setWorkflowNodeName("oracle_1");

        workflowNodeTestOutputRepository.save(workflowNodeTestOutput);

        WorkflowNodeTestOutput savedWorkflowNodeTestOutput = workflowNodeTestOutputRepository
            .findByWorkflowIdAndWorkflowNodeNameAndEnvironmentId("workflow1", "oracle_1", 0L)
            .orElseThrow();

        assertThat(savedWorkflowNodeTestOutput.isSampleOutputTruncated()).isTrue();
        assertThat(savedWorkflowNodeTestOutput.getSampleOutput()).isEqualTo(List.of("first", "second"));
    }

    @Configuration
    @EnableAutoConfiguration
    @Import({
        JacksonConfiguration.class, LiquibaseConfiguration.class, PostgreSQLContainerConfiguration.class,
        TestConfiguration.TestJdbcConfiguration.class
    })
    static class TestConfiguration {

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        static class TestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

            private final ObjectMapper objectMapper;

            @SuppressFBWarnings("EI2")
            TestJdbcConfiguration(ObjectMapper objectMapper) {
                this.objectMapper = objectMapper;
            }

            @Override
            protected List<?> userConverters() {
                return Arrays.asList(
                    new MapWrapperToStringConverter(objectMapper), new StringToMapWrapperConverter(objectMapper));
            }
        }
    }
}
