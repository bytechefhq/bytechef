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

package com.bytechef.platform.workflow.execution.teardown;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.scheduler.TriggerScheduler;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SuspendedJobTenantTeardownContributorTest {

    private final JobService jobService = mock(JobService.class);
    private final TriggerScheduler triggerScheduler = mock(TriggerScheduler.class);
    private final SuspendedJobTenantTeardownContributor contributor = new SuspendedJobTenantTeardownContributor(
        jobService, triggerScheduler);

    @Test
    void testCancelsOneTimeTaskOfEveryStoppedJob() {
        when(jobService.getJobIds(Job.Status.STOPPED)).thenReturn(List.of(7L, 9L));

        assertThat(contributor.onDeletionRequested("000001")).isEmpty();

        verify(triggerScheduler).cancelOneTimeTask(7L);
        verify(triggerScheduler).cancelOneTimeTask(9L);
    }

    @Test
    void testFailingCancelIsRecordedAndDoesNotStopTheNext() {
        when(jobService.getJobIds(Job.Status.STOPPED)).thenReturn(List.of(7L, 9L));

        doThrow(new UnsupportedOperationException("not supported"))
            .when(triggerScheduler)
            .cancelOneTimeTask(7L);

        assertThat(contributor.onDeletionRequested("000001")).containsExactly("job 7: not supported");

        verify(triggerScheduler).cancelOneTimeTask(9L);
    }
}
