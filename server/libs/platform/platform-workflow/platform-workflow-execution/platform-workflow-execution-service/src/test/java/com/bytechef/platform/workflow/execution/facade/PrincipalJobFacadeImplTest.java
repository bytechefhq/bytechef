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

package com.bytechef.platform.workflow.execution.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.LicenceJobUsageService;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests {@link PrincipalJobFacadeImpl#createPrincipalLinkedJob} -- the new method added for the agent-tool sub-workflow
 * bridge. Covers both branches (principal present, principal missing) -- the missing-principal branch MUST throw rather
 * than silently create an orphan job (review finding C3).
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class PrincipalJobFacadeImplTest {

    @Mock
    private PrincipalJobService principalJobService;

    @Mock
    private JobFacade jobFacade;

    @Mock
    private JobService jobService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private LicenceJobUsageService licenceJobUsageService;

    @Test
    void testCreatePrincipalLinkedJobCreatesJobAndLinksPrincipal() {
        long referenceJobId = 100L;
        long principalId = 7L;
        long newJobId = 200L;

        JobParametersDTO jobParametersDTO = new JobParametersDTO("wf-99", Map.of(), Map.of());

        when(principalJobService.fetchJobPrincipalId(referenceJobId, PlatformType.AUTOMATION))
            .thenReturn(Optional.of(principalId));
        when(jobFacade.createJob(jobParametersDTO)).thenReturn(newJobId);

        PrincipalJobFacadeImpl facade = new PrincipalJobFacadeImpl(
            principalJobService, jobFacade, jobService, workflowService, licenceJobUsageService);

        long result = facade.createPrincipalLinkedJob(referenceJobId, jobParametersDTO, PlatformType.AUTOMATION);

        assertEquals(newJobId, result);
        verify(principalJobService).create(newJobId, principalId, PlatformType.AUTOMATION);
    }

    /**
     * C3 regression: if the reference job has no principal, the facade must throw rather than silently create a job
     * with no tenant/billing attribution. The old behavior was log-and-continue, which produced orphan jobs invisible
     * to workspace-scoped lookups -- a silent multi-tenant data leak.
     */
    @Test
    void testCreatePrincipalLinkedJobThrowsWhenPrincipalMissing() {
        long referenceJobId = 100L;

        JobParametersDTO jobParametersDTO = new JobParametersDTO("wf-99", Map.of(), Map.of());

        when(principalJobService.fetchJobPrincipalId(referenceJobId, PlatformType.AUTOMATION))
            .thenReturn(Optional.empty());

        PrincipalJobFacadeImpl facade = new PrincipalJobFacadeImpl(
            principalJobService, jobFacade, jobService, workflowService, licenceJobUsageService);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> facade.createPrincipalLinkedJob(referenceJobId, jobParametersDTO, PlatformType.AUTOMATION));

        // No job must have been created -- C3 requires fail-fast, not create-then-orphan.

        verify(jobFacade, never()).createJob(any(JobParametersDTO.class));
        verify(principalJobService, never()).create(anyLong(), anyLong(), any());

        // Sanity-check the exception message gives operators what they need to diagnose.

        assertEquals(true, exception.getMessage()
            .contains(String.valueOf(referenceJobId)));
    }
}
