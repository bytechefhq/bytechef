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

package com.bytechef.automation.configuration.facade;

import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.config.ApplicationProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ProjectErrorWorkflowFacadeTest {

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private ErrorWorkflowConfigurationValidator errorWorkflowConfigurationValidator;

    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ProjectFacadeImpl projectFacade;

    @Test
    void testValidatesBeforeSaving() {
        projectFacade.updateProjectErrorWorkflow(1L, 5L);

        Mockito.verify(errorWorkflowConfigurationValidator)
            .validate(1L, 5L, null);
        Mockito.verify(projectService)
            .updateErrorWorkflow(1L, 5L);
    }

    @Test
    void testClearingSkipsValidation() {
        projectFacade.updateProjectErrorWorkflow(1L, null);

        Mockito.verifyNoInteractions(errorWorkflowConfigurationValidator);
        Mockito.verify(projectService)
            .updateErrorWorkflow(1L, null);
    }

    @Test
    void testRejectedReferenceIsNotSaved() {
        Mockito.doThrow(new IllegalArgumentException("nope"))
            .when(errorWorkflowConfigurationValidator)
            .validate(1L, 5L, null);

        Assertions.assertThrows(
            IllegalArgumentException.class, () -> projectFacade.updateProjectErrorWorkflow(1L, 5L));
        Mockito.verify(projectService, Mockito.never())
            .updateErrorWorkflow(ArgumentMatchers.anyLong(), ArgumentMatchers.any());
    }
}
