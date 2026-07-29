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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.atlas.coordinator.event.DeleteJobEvent;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SuspendedTaskStateJobDeletionListenerTest {

    private static final String JOB_RESUME_ID = EncodingUtils.base64EncodeToString(
        "public:42:00000000-0000-0000-0000-000000000001");

    @Test
    void testDeletesTaskStateWhenJobResumeIdPresent() {
        TaskStateService taskStateService = mock(TaskStateService.class);

        SuspendedTaskStateJobDeletionListener listener = new SuspendedTaskStateJobDeletionListener(taskStateService);

        listener.onDeleteJob(new DeleteJobEvent(42L, Map.of(MetadataConstants.JOB_RESUME_ID, JOB_RESUME_ID)));

        verify(taskStateService).delete(JobResumeId.parse(JOB_RESUME_ID));
    }

    @Test
    void testNoOpWhenJobResumeIdAbsent() {
        TaskStateService taskStateService = mock(TaskStateService.class);

        SuspendedTaskStateJobDeletionListener listener = new SuspendedTaskStateJobDeletionListener(taskStateService);

        // A non-suspended job carries no jobResumeId — nothing to clean up.
        listener.onDeleteJob(new DeleteJobEvent(42L, Map.of()));

        verify(taskStateService, never()).delete(any());
    }

    @Test
    void testNoOpWhenTaskStateServiceAbsent() {
        SuspendedTaskStateJobDeletionListener listener = new SuspendedTaskStateJobDeletionListener(null);

        // Deployments without task_state persistence must not fail job deletion.
        listener.onDeleteJob(new DeleteJobEvent(42L, Map.of(MetadataConstants.JOB_RESUME_ID, JOB_RESUME_ID)));
    }

    @Test
    void testSwallowsDeletionFailureSoJobDeletionProceeds() {
        TaskStateService taskStateService = mock(TaskStateService.class);

        doThrow(new RuntimeException("db down")).when(taskStateService)
            .delete(any());

        SuspendedTaskStateJobDeletionListener listener = new SuspendedTaskStateJobDeletionListener(taskStateService);

        // A cleanup failure must never propagate out of the job-deletion transaction.
        listener.onDeleteJob(new DeleteJobEvent(42L, Map.of(MetadataConstants.JOB_RESUME_ID, JOB_RESUME_ID)));
    }
}
