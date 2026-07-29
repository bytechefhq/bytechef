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

package com.bytechef.platform.workflow.test.coordinator.task.dispatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class SimulationTaskDispatcherPreSendProcessorTest {

    private static final long JOB_ID = 1L;

    @Mock
    private JobService jobService;

    private SimulationTaskDispatcherPreSendProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new SimulationTaskDispatcherPreSendProcessor(jobService);
    }

    @Test
    void processCopiesDryRunTrueFromJobMetadata() {
        TaskExecution taskExecution = createTaskExecution();

        Job job = mock(Job.class);

        doReturn(Map.of(MetadataConstants.DRY_RUN, true, MetadataConstants.EDITOR_ENVIRONMENT, true))
            .when(job)
            .getMetadata();
        when(jobService.getJob(JOB_ID)).thenReturn(job);

        TaskExecution result = processor.process(taskExecution);

        Map<String, ?> metadata = result.getMetadata();

        assertThat(metadata.get(MetadataConstants.DRY_RUN)).isEqualTo(true);
        assertThat(metadata.get(MetadataConstants.EDITOR_ENVIRONMENT)).isEqualTo(true);
    }

    @Test
    void processSetsDryRunFalseWhenAbsentFromJobMetadata() {
        TaskExecution taskExecution = createTaskExecution();

        Job job = mock(Job.class);

        doReturn(Map.of()).when(job)
            .getMetadata();
        when(jobService.getJob(JOB_ID)).thenReturn(job);

        TaskExecution result = processor.process(taskExecution);

        Map<String, ?> metadata = result.getMetadata();

        assertThat(metadata.get(MetadataConstants.DRY_RUN)).isEqualTo(false);
        assertThat(metadata.get(MetadataConstants.EDITOR_ENVIRONMENT)).isEqualTo(false);
    }

    @Test
    void canProcessReturnsTrue() {
        assertThat(processor.canProcess(createTaskExecution())).isTrue();
    }

    private static TaskExecution createTaskExecution() {
        return TaskExecution.builder()
            .jobId(JOB_ID)
            .workflowTask(mock(WorkflowTask.class))
            .build();
    }
}
