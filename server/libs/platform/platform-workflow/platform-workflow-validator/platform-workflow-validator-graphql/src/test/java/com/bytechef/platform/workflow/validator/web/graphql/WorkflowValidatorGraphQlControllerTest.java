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

package com.bytechef.platform.workflow.validator.web.graphql;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkflowValidatorGraphQlControllerTest {

    private final WorkflowValidatorFacade workflowValidatorFacade = mock(WorkflowValidatorFacade.class);
    private final WorkflowValidatorGraphQlController controller =
        new WorkflowValidatorGraphQlController(workflowValidatorFacade);

    @Test
    void passesEnvironmentThroughWhenGiven() {
        controller.validateWorkflow("{}", 2L);

        verify(workflowValidatorFacade).validateWorkflow("{}", 2L);
    }

    @Test
    void usesFacadeDefaultWhenEnvironmentIsAbsent() {
        controller.validateWorkflow("{}", null);

        verify(workflowValidatorFacade).validateWorkflow("{}");
    }

    @Test
    void validateByIdPassesEnvironmentThrough() {
        controller.validateWorkflowById("wf-1", 1L);

        verify(workflowValidatorFacade).validateWorkflowById("wf-1", 1L);
    }
}
