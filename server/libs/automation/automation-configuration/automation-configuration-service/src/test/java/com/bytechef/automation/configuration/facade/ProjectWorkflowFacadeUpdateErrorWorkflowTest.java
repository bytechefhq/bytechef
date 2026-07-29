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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
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
class ProjectWorkflowFacadeUpdateErrorWorkflowTest {

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private ErrorWorkflowConfigurationValidator errorWorkflowConfigurationValidator;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @InjectMocks
    private ProjectWorkflowFacadeImpl projectWorkflowFacade;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // The facade verifies the mutated workflow belongs to the authorized project before doing anything else.
        Mockito.lenient()
            .when(projectWorkflowService.getProjectWorkflow(10L))
            .thenReturn(new ProjectWorkflow(1L, 1, "wf-10"));
    }

    @Test
    void testCrossProjectSetIsRejectedAndNothingSaved() {
        Mockito.when(projectWorkflowService.getProjectWorkflow(20L))
            .thenReturn(new ProjectWorkflow(2L, 1, "wf-20"));

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> projectWorkflowFacade.updateWorkflowErrorWorkflow(1L, 20L, 99L, false));

        verifyNoInteractions(errorWorkflowConfigurationValidator);
        verify(projectWorkflowService, never())
            .updateErrorWorkflow(anyLong(), ArgumentMatchers.any(), Mockito.anyBoolean());
    }

    @Test
    void testCrossProjectClearIsRejectedAndNothingSaved() {
        Mockito.when(projectWorkflowService.getProjectWorkflow(20L))
            .thenReturn(new ProjectWorkflow(2L, 1, "wf-20"));

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> projectWorkflowFacade.updateWorkflowErrorWorkflow(1L, 20L, null, true));

        verify(projectWorkflowService, never())
            .updateErrorWorkflow(anyLong(), ArgumentMatchers.any(), Mockito.anyBoolean());
    }

    @Test
    void testValidatesBeforeSaving() {
        projectWorkflowFacade.updateWorkflowErrorWorkflow(1L, 10L, 99L, false);

        verify(errorWorkflowConfigurationValidator).validate(1L, 99L, 10L);
        verify(projectWorkflowService).updateErrorWorkflow(10L, 99L, false);
    }

    @Test
    void testClearingSkipsValidation() {
        projectWorkflowFacade.updateWorkflowErrorWorkflow(1L, 10L, null, false);

        verifyNoInteractions(errorWorkflowConfigurationValidator);
        verify(projectWorkflowService).updateErrorWorkflow(10L, null, false);
    }

    @Test
    void testDisablingWithNoReferenceSkipsValidation() {
        projectWorkflowFacade.updateWorkflowErrorWorkflow(1L, 10L, null, true);

        verifyNoInteractions(errorWorkflowConfigurationValidator);
        verify(projectWorkflowService).updateErrorWorkflow(10L, null, true);
    }

    @Test
    void testRejectedReferenceIsNotSaved() {
        doThrow(new IllegalArgumentException("nope"))
            .when(errorWorkflowConfigurationValidator)
            .validate(1L, 99L, 10L);

        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> projectWorkflowFacade.updateWorkflowErrorWorkflow(1L, 10L, 99L, false));

        verify(projectWorkflowService, never())
            .updateErrorWorkflow(anyLong(), ArgumentMatchers.any(), Mockito.anyBoolean());
    }
}
