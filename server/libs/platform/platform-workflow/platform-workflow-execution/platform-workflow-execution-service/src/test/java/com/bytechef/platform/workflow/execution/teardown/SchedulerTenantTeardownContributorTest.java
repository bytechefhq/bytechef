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

import com.bytechef.platform.scheduler.ConnectionRefreshScheduler;
import com.bytechef.platform.scheduler.TriggerScheduler;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SchedulerTenantTeardownContributorTest {

    private final ConnectionRefreshScheduler connectionRefreshScheduler = mock(ConnectionRefreshScheduler.class);
    private final TriggerScheduler triggerScheduler = mock(TriggerScheduler.class);
    private final SchedulerTenantTeardownContributor contributor = new SchedulerTenantTeardownContributor(
        connectionRefreshScheduler, triggerScheduler);

    @Test
    void testCancelsTriggerAndConnectionRefreshJobsForTenant() {
        assertThat(contributor.onDeletionRequested("000001")).isEmpty();

        verify(triggerScheduler).cancelAllForTenant("000001");
        verify(connectionRefreshScheduler).cancelAllForTenant("000001");
    }

    @Test
    void testTriggerSchedulerFailureDoesNotSkipConnectionRefresh() {
        doThrow(new IllegalStateException("scheduler unavailable"))
            .when(triggerScheduler)
            .cancelAllForTenant("000001");

        assertThat(contributor.onDeletionRequested("000001"))
            .containsExactly("triggerScheduler: scheduler unavailable");

        verify(connectionRefreshScheduler).cancelAllForTenant("000001");
    }
}
