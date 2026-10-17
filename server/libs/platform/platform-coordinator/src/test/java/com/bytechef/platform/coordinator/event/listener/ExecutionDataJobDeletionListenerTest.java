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

package com.bytechef.platform.coordinator.event.listener;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.event.DeleteJobEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.facade.JobFacadeImpl;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.data.storage.domain.DataStorageScope;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
class ExecutionDataJobDeletionListenerTest {

    @Test
    void testDeletesCurrentExecutionDataOfDeletedJob() {
        DataStorage dataStorage = mock(DataStorage.class);

        ExecutionDataJobDeletionListener listener = new ExecutionDataJobDeletionListener(dataStorage);

        listener.onDeleteJob(new DeleteJobEvent(42L, Map.of()));

        verify(dataStorage).deleteScopeData(DataStorageScope.CURRENT_EXECUTION, "42");
    }

    @Test
    void testNoOpWhenDataStorageAbsent() {
        ExecutionDataJobDeletionListener listener = new ExecutionDataJobDeletionListener(null);

        listener.onDeleteJob(new DeleteJobEvent(42L, Map.of()));
    }

    @Test
    void testIgnoresProviderWithoutScopeWideDelete() {
        DataStorage dataStorage = mock(DataStorage.class);

        doThrow(new UnsupportedOperationException()).when(dataStorage)
            .deleteScopeData(any(), any());

        ExecutionDataJobDeletionListener listener = new ExecutionDataJobDeletionListener(dataStorage);

        listener.onDeleteJob(new DeleteJobEvent(42L, Map.of()));
    }

    @Test
    void testPropagatesDeletionFailure() {
        DataStorage dataStorage = mock(DataStorage.class);

        doThrow(new IllegalStateException("db down")).when(dataStorage)
            .deleteScopeData(any(), any());

        ExecutionDataJobDeletionListener listener = new ExecutionDataJobDeletionListener(dataStorage);

        assertThatThrownBy(() -> listener.onDeleteJob(new DeleteJobEvent(42L, Map.of())))
            .isInstanceOf(IllegalStateException.class);
    }

    @Nested
    class DeleteJobThroughJobFacadeTest {

        private static final long CHILD_JOB_ID = 2L;
        private static final long PARENT_JOB_ID = 1L;

        private final DataStorage dataStorage = mock(DataStorage.class);
        private final JobService jobService = mock(JobService.class);

        private final JobFacade jobFacade = createJobFacade();

        @Test
        void testDeletesCurrentExecutionDataOfJobAndChildJobBeforeTheirRows() {
            jobFacade.deleteJob(PARENT_JOB_ID);

            InOrder inOrder = inOrder(dataStorage, jobService);

            inOrder.verify(dataStorage)
                .deleteScopeData(DataStorageScope.CURRENT_EXECUTION, String.valueOf(CHILD_JOB_ID));
            inOrder.verify(jobService)
                .deleteJob(CHILD_JOB_ID);
            inOrder.verify(dataStorage)
                .deleteScopeData(DataStorageScope.CURRENT_EXECUTION, String.valueOf(PARENT_JOB_ID));
            inOrder.verify(jobService)
                .deleteJob(PARENT_JOB_ID);
        }

        @Test
        void testFailedDataDeletionKeepsTheJobRow() {
            doThrow(new IllegalStateException("db down")).when(dataStorage)
                .deleteScopeData(DataStorageScope.CURRENT_EXECUTION, String.valueOf(PARENT_JOB_ID));

            assertThatThrownBy(() -> jobFacade.deleteJob(PARENT_JOB_ID))
                .isInstanceOf(IllegalStateException.class);

            verify(jobService, never()).deleteJob(PARENT_JOB_ID);
        }

        private JobFacade createJobFacade() {
            ExecutionDataJobDeletionListener listener = new ExecutionDataJobDeletionListener(dataStorage);

            ApplicationEventPublisher eventPublisher = event -> {
                if (event instanceof DeleteJobEvent deleteJobEvent) {
                    listener.onDeleteJob(deleteJobEvent);
                }
            };

            when(jobService.getChildJobIds(PARENT_JOB_ID)).thenReturn(List.of(CHILD_JOB_ID));
            when(jobService.getJob(anyLong())).thenAnswer(invocation -> new Job(invocation.getArgument(0, Long.class)));

            return new JobFacadeImpl(
                eventPublisher, mock(ContextService.class), jobService, mock(TaskExecutionService.class),
                mock(TaskFileStorage.class), mock(WorkflowService.class));
        }
    }
}
