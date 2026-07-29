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

package com.bytechef.automation.configuration.service;

import com.bytechef.automation.configuration.audit.ProjectWorkflowAuditPublisher;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ProjectWorkflowServiceUpdateErrorWorkflowTest {

    @Mock
    private ProjectWorkflowAuditPublisher projectWorkflowAuditPublisher;

    @Mock
    private ProjectWorkflowRepository projectWorkflowRepository;

    @InjectMocks
    private ProjectWorkflowServiceImpl projectWorkflowService;

    @Test
    void testUpdateErrorWorkflowSetsBothFieldsAndSaves() {
        ProjectWorkflow existing = new ProjectWorkflow(10L);

        Mockito.when(projectWorkflowRepository.findById(10L))
            .thenReturn(Optional.of(existing));
        Mockito.when(projectWorkflowRepository.save(existing))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectWorkflow result = projectWorkflowService.updateErrorWorkflow(10L, 99L, true);

        Assertions.assertEquals(99L, result.getErrorProjectWorkflowId());
        Assertions.assertTrue(result.isErrorWorkflowDisabled());
    }

    @Test
    void testUpdateErrorWorkflowAcceptsNullReference() {
        ProjectWorkflow existing = new ProjectWorkflow(10L);

        existing.setErrorProjectWorkflowId(5L);

        Mockito.when(projectWorkflowRepository.findById(10L))
            .thenReturn(Optional.of(existing));
        Mockito.when(projectWorkflowRepository.save(existing))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ProjectWorkflow result = projectWorkflowService.updateErrorWorkflow(10L, null, false);

        Assertions.assertNull(result.getErrorProjectWorkflowId());
        Assertions.assertFalse(result.isErrorWorkflowDisabled());
    }

    @Test
    void testUpdateErrorWorkflowThrowsWhenNotFound() {
        Mockito.when(projectWorkflowRepository.findById(10L))
            .thenReturn(Optional.empty());

        Assertions.assertThrows(
            IllegalArgumentException.class, () -> projectWorkflowService.updateErrorWorkflow(10L, 99L, false));

        Mockito.verify(projectWorkflowRepository, Mockito.never())
            .save(ArgumentCaptor.forClass(ProjectWorkflow.class)
                .capture());
    }
}
