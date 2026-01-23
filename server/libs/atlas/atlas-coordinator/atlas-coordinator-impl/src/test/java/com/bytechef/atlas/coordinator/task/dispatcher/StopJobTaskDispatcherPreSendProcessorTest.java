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

package com.bytechef.atlas.coordinator.task.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Matija Petanjek
 */
public class StopJobTaskDispatcherPreSendProcessorTest {

    private JobService jobService;
    private StopJobTaskDispatcherPreSendProcessor processor;
    private TaskExecution taskExecution;

    @BeforeEach
    void beforeEach() {
        jobService = mock(JobService.class);

        processor = new StopJobTaskDispatcherPreSendProcessor(jobService);

        taskExecution = TaskExecution.builder()
            .jobId(RandomUtils.nextLong())
            .status(TaskExecution.Status.STARTED)
            .build();
    }

    @Test
    public void testProcessWithStartedJob() {
        taskExecution = process(Job.Status.STARTED);

        assertThat(taskExecution.getStatus()).isEqualTo(TaskExecution.Status.STARTED);
    }

    @Test
    public void testProcessWithStoppedJob() {
        taskExecution = process(Job.Status.STOPPED);

        assertThat(taskExecution.getStatus()).isEqualTo(TaskExecution.Status.CANCELLED);
    }

    private TaskExecution process(Job.Status status) {
        Job job = new Job();
        job.setStatus(status);

        when(jobService.getJob(anyLong())).thenReturn(job);

        return processor.process(taskExecution);
    }

}
