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

package com.bytechef.automation.configuration.subflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ChildJobPrincipalFactoryImplTest {

    @Test
    void testCreateChildJobDelegatesToPrincipalJobFacade() {
        PrincipalJobFacade principalJobFacade = mock(PrincipalJobFacade.class);

        long parentJobId = 10L;
        long childJobId = 20L;

        JobParametersDTO jobParametersDTO = new JobParametersDTO("wf-1", Map.of(), Map.of());

        when(principalJobFacade.createChildJob(parentJobId, jobParametersDTO, PlatformType.AUTOMATION))
            .thenReturn(childJobId);

        ChildJobPrincipalFactoryImpl factory = new ChildJobPrincipalFactoryImpl(principalJobFacade);

        long result = factory.createChildJob(parentJobId, jobParametersDTO);

        assertEquals(childJobId, result);
        verify(principalJobFacade).createChildJob(parentJobId, jobParametersDTO, PlatformType.AUTOMATION);
    }

    @Test
    void testCreatePrincipalLinkedJobUsesReferenceJobPrincipal() {
        PrincipalJobFacade principalJobFacade = mock(PrincipalJobFacade.class);

        long referenceJobId = 100L;
        long newJobId = 200L;

        JobParametersDTO jobParametersDTO =
            new JobParametersDTO("wf-99", Map.of("inputs", Map.of("amount", 5)), Map.of());

        when(principalJobFacade.createPrincipalLinkedJob(referenceJobId, jobParametersDTO, PlatformType.AUTOMATION))
            .thenReturn(newJobId);

        ChildJobPrincipalFactoryImpl factory = new ChildJobPrincipalFactoryImpl(principalJobFacade);

        long result = factory.createPrincipalLinkedJob(referenceJobId, jobParametersDTO);

        assertEquals(newJobId, result);
        verify(principalJobFacade).createPrincipalLinkedJob(referenceJobId, jobParametersDTO, PlatformType.AUTOMATION);
    }
}
