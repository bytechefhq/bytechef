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

package com.bytechef.platform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.platform.workflow.validator.exception.WorkflowValidatorErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies that the workflow save path rejects duplicate node names by reusing {@link WorkflowValidatorFacade}.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkflowFacadeDuplicateNodeNamesTest {

    @Mock
    private ComponentConnectionFacade componentConnectionFacade;

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private WorkflowValidatorFacade workflowValidatorFacade;

    private WorkflowFacade workflowFacade;

    @BeforeEach
    void beforeEach() {
        workflowFacade = new WorkflowFacadeImpl(
            componentConnectionFacade, componentDefinitionService, workflowValidatorFacade, workflowService);
    }

    @Test
    void testUpdateRejectsDuplicateNodeNamesAndDoesNotPersist() {
        doThrow(
            new ConfigurationException(
                "Workflow node names must be unique. Duplicate node names: task_1",
                WorkflowValidatorErrorType.DUPLICATE_NODE_NAMES))
                    .when(workflowValidatorFacade)
                    .validateNoDuplicateNodeNames(anyString());

        assertThatThrownBy(() -> workflowFacade.update("workflow-1", "{}", 1))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("task_1");

        verify(workflowService, never()).update(anyString(), anyString(), anyInt());
    }

    @Test
    void testUpdatePersistsWhenNodeNamesAreUnique() {
        workflowFacade.update("workflow-1", "{}", 1);

        verify(workflowValidatorFacade).validateNoDuplicateNodeNames("{}");
        verify(workflowService).update("workflow-1", "{}", 1);
    }
}
