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

package com.bytechef.atlas.execution.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
class JobFacadeTest {

    private static final long JOB_ID = 7L;

    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final JobService jobService = mock(JobService.class);
    private final JobFacade jobFacade = new JobFacadeImpl(
        eventPublisher, mock(ContextService.class), jobService, mock(TaskExecutionService.class),
        mock(TaskFileStorage.class), mock(WorkflowService.class));

    @Test
    void testResumeJobStartsJobBeforePublishingResumeEvent() {
        jobFacade.resumeJob(JOB_ID);

        InOrder inOrder = inOrder(jobService, eventPublisher);

        ArgumentCaptor<ResumeJobEvent> resumeJobEventArgumentCaptor = ArgumentCaptor.forClass(ResumeJobEvent.class);

        inOrder.verify(jobService)
            .resumeToStatusStarted(JOB_ID);
        inOrder.verify(eventPublisher)
            .publishEvent(resumeJobEventArgumentCaptor.capture());

        ResumeJobEvent resumeJobEvent = resumeJobEventArgumentCaptor.getValue();

        assertThat(resumeJobEvent.getJobId()).isEqualTo(JOB_ID);
        assertThat(resumeJobEvent.isStarted()).isTrue();
        assertThat(resumeJobEvent.getTaskExecutionId()).isNull();
    }

    @Test
    void testResumeJobPublishesNothingWhenJobCannotBeResumed() {
        when(jobService.resumeToStatusStarted(JOB_ID))
            .thenThrow(new IllegalArgumentException("can't resume job 7 as it is COMPLETED"));

        assertThatThrownBy(() -> jobFacade.resumeJob(JOB_ID))
            .isInstanceOf(IllegalArgumentException.class);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
