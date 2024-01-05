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

package com.bytechef.atlas.execution.repository.jdbc;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Counter;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.jdbc.config.WorkflowExecutionRepositoryIntTestConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = WorkflowExecutionRepositoryIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class JdbcCounterRepositoryIntTest {

    @Autowired
    private JdbcCounterRepository counterRepository;

    @Autowired
    private JdbcJobRepository jobRepository;

    @Autowired
    private JdbcTaskExecutionRepository taskExecutionRepository;

    private Counter counter;

    @AfterEach
    public void afterEach() {
        counterRepository.deleteAll();

        taskExecutionRepository.deleteAll();
        jobRepository.deleteAll();

    }

    @BeforeEach
    public void beforeEach() {
        Job job = jobRepository.save(getJob());

        TaskExecution taskExecution = getTaskExecution(Validate.notNull(job.getId(), "id"));

        taskExecution = taskExecutionRepository.save(taskExecution);

        counter = getCounter(Validate.notNull(taskExecution.getId(), "id"));

        counter = counterRepository.save(counter);
    }

    @Test
    public void testFindValueById() {
        Long value = counterRepository.findValueByIdForUpdate(counter.getId());

        Assertions.assertEquals(counter.getValue(), value);
    }

    @Test
    public void testUpdate() {
        Long value = counterRepository.findValueByIdForUpdate(counter.getId());

        Assertions.assertEquals(counter.getValue(), value);

        counterRepository.update(counter.getId(), 5);

        value = counterRepository.findValueByIdForUpdate(counter.getId());

        Assertions.assertEquals(5, value);
    }

    @SuppressFBWarnings("DMI")
    private static Counter getCounter(long taskExecutionId) {
        Counter counter = new Counter();

        counter.setId(taskExecutionId);
        counter.setValue(3L);

        return counter;
    }

    private static Job getJob() {
        Job job = new Job();

        job.setStatus(Job.Status.CREATED);
        job.setWorkflowId("demo:1234");

        return job;
    }

    private static TaskExecution getTaskExecution(long jobId) {
        return TaskExecution.builder()
            .jobId(jobId)
            .workflowTask(new WorkflowTask(Map.of(WorkflowConstants.NAME, "name", WorkflowConstants.TYPE, "type")))
            .build();
    }
}
