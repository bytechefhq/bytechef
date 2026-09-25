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

package com.bytechef.automation.task.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.repository.ApprovalTaskRepository;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ApprovalTaskEnvironmentResolverTest {

    private static final long APPROVAL_TASK_ID = 3L;

    private final ApprovalTaskRepository approvalTaskRepository = mock(ApprovalTaskRepository.class);
    private final ApprovalTaskEnvironmentResolver resolver =
        new ApprovalTaskEnvironmentResolver(approvalTaskRepository);

    @Test
    void testResourceTypeMatchesTheOwnershipResolver() {
        assertThat(resolver.resourceType()).isEqualTo("ApprovalTask");
    }

    @Test
    void testFetchEnvironmentReturnsTheEnvironmentRecordedForAJobsTask() {
        ApprovalTask approvalTask = ApprovalTask.builder()
            .jobResumeId("job-resume-id")
            .environment(Environment.PRODUCTION)
            .build();

        when(approvalTaskRepository.findById(APPROVAL_TASK_ID)).thenReturn(Optional.of(approvalTask));

        assertThat(resolver.fetchEnvironment(APPROVAL_TASK_ID)).contains(Environment.PRODUCTION);
    }

    @Test
    void testFetchEnvironmentIsEmptyForATaskRaisedForNoJob() {
        ApprovalTask approvalTask = ApprovalTask.builder()
            .environment(Environment.PRODUCTION)
            .build();

        when(approvalTaskRepository.findById(APPROVAL_TASK_ID)).thenReturn(Optional.of(approvalTask));

        assertThat(resolver.fetchEnvironment(APPROVAL_TASK_ID)).isEmpty();
    }

    @Test
    void testFetchEnvironmentIsEmptyForAnUnknownTask() {
        when(approvalTaskRepository.findById(APPROVAL_TASK_ID)).thenReturn(Optional.empty());

        assertThat(resolver.fetchEnvironment(APPROVAL_TASK_ID)).isEmpty();
    }

    @Test
    void testFetchEnvironmentIsEmptyForANonNumericId() {
        assertThat(resolver.fetchEnvironment("not-a-number")).isEmpty();
    }
}
