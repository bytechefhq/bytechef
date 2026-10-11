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
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class PrincipalJobFacadeTest {

    @Mock
    private PrincipalJobService principalJobService;

    @Mock
    private JobFacade jobFacade;

    @Mock
    private JobService jobService;

    @Mock
    private WorkflowService workflowService;

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
            principalJobService, jobFacade, jobService, workflowService);

        long result = facade.createPrincipalLinkedJob(referenceJobId, jobParametersDTO, PlatformType.AUTOMATION);

        assertEquals(newJobId, result);
        verify(principalJobService).create(newJobId, principalId, PlatformType.AUTOMATION);
    }

    @Test
    void testCreatePrincipalLinkedJobThrowsWhenPrincipalMissing() {
        long referenceJobId = 100L;

        JobParametersDTO jobParametersDTO = new JobParametersDTO("wf-99", Map.of(), Map.of());

        when(principalJobService.fetchJobPrincipalId(referenceJobId, PlatformType.AUTOMATION))
            .thenReturn(Optional.empty());

        PrincipalJobFacadeImpl facade = new PrincipalJobFacadeImpl(
            principalJobService, jobFacade, jobService, workflowService);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> facade.createPrincipalLinkedJob(referenceJobId, jobParametersDTO, PlatformType.AUTOMATION));

        verify(jobFacade, never()).createJob(any(JobParametersDTO.class));
        verify(principalJobService, never()).create(anyLong(), anyLong(), any());

        assertEquals(true, exception.getMessage()
            .contains(String.valueOf(referenceJobId)));
    }

    @Test
    void testCreatePrincipalLinkedJobRejectsASubflowChild() {
        JobParametersDTO jobParametersDTO = new JobParametersDTO("wf-99", 10L, Map.of());

        PrincipalJobFacadeImpl facade = new PrincipalJobFacadeImpl(
            principalJobService, jobFacade, jobService, workflowService);

        assertThrows(
            IllegalArgumentException.class,
            () -> facade.createPrincipalLinkedJob(100L, jobParametersDTO, PlatformType.AUTOMATION));

        verify(jobFacade, never()).createJob(any(JobParametersDTO.class));
    }
}
