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

package com.bytechef.atlas.execution.repository.jdbc;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.jdbc.config.WorkflowExecutionRepositoryIntTestConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Map;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = WorkflowExecutionRepositoryIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class JdbcTaskExecutionRepositoryIntTest {

    @Autowired
    private JdbcJobRepository jobRepository;

    @Autowired
    private JdbcTaskExecutionRepository taskExecutionRepository;

    @AfterEach
    public void afterEach() {
        taskExecutionRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    public void testSaveOfAStaleTaskExecutionFailsWithAnOptimisticLockingFailure() {
        long taskExecutionId = Validate.notNull(taskExecutionRepository.save(getTaskExecution())
            .getId(), "id");

        TaskExecution firstTaskExecution = taskExecutionRepository.findById(taskExecutionId)
            .orElseThrow();
        TaskExecution secondTaskExecution = taskExecutionRepository.findById(taskExecutionId)
            .orElseThrow();

        firstTaskExecution.putMetadata("consumedBy", "first");

        taskExecutionRepository.save(firstTaskExecution);

        secondTaskExecution.putMetadata("consumedBy", "second");

        Assertions.assertThrows(
            OptimisticLockingFailureException.class, () -> taskExecutionRepository.save(secondTaskExecution));

        TaskExecution storedTaskExecution = taskExecutionRepository.findById(taskExecutionId)
            .orElseThrow();

        Assertions.assertEquals("first", storedTaskExecution.getMetadata()
            .get("consumedBy"));
        Assertions.assertEquals(firstTaskExecution.getVersion(), storedTaskExecution.getVersion());
    }

    private TaskExecution getTaskExecution() {
        Job job = new Job();

        job.setStatus(Job.Status.STARTED);
        job.setWorkflowId("demo:1234");

        job = jobRepository.save(job);

        TaskExecution taskExecution = TaskExecution.builder()
            .workflowTask(new WorkflowTask(Map.of("name", "task1", "type", "test/v1/noop")))
            .build();

        taskExecution.setJobId(job.getId());
        taskExecution.setTaskNumber(1);

        return taskExecution;
    }
}
