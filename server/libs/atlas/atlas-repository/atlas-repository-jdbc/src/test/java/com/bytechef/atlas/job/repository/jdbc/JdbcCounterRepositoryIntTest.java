
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.atlas.job.repository.jdbc;

import com.bytechef.atlas.domain.Counter;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.job.repository.jdbc.config.WorkflowRepositoryIntTestConfiguration;
import com.bytechef.atlas.repository.CounterRepository;
import com.bytechef.atlas.repository.JobRepository;
import com.bytechef.atlas.repository.TaskExecutionRepository;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.test.annotation.EmbeddedSql;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest(
    classes = WorkflowRepositoryIntTestConfiguration.class,
    properties = "bytechef.workflow.persistence.provider=jdbc")
public class JdbcCounterRepositoryIntTest {

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    private Counter counter;

    @BeforeEach
    @SuppressFBWarnings("NP")
    public void beforeEach() {
        Job job = jobRepository.save(getJob());

        TaskExecution taskExecution = getTaskExecution(job.getId());

        taskExecution = taskExecutionRepository.save(taskExecution);

        counter = getCounter(taskExecution.getId());

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
        counter.setNew(true);
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
        return TaskExecution.of(jobId, new WorkflowTask());
    }
}
