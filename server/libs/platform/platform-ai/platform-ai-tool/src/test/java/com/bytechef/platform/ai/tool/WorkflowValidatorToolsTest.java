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

package com.bytechef.platform.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.ai.tool.WorkflowValidatorTools.WorkflowValidationResult;
import com.bytechef.platform.ai.tool.util.AllowedComponentNamesUtils;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowValidatorToolsTest {

    private static final String WORKFLOW = """
        {
            "label": "Flow",
            "tasks": [
                {"name": "condition_1", "type": "condition/v1", "parameters": {"caseTrue": [
                    {"name": "salesforce_1", "type": "salesforce/v1/createRecord"}
                ]}},
                {"name": "slack_1", "type": "slack/v1/sendMessage"}
            ]
        }""";

    private final ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
    private final WorkflowValidatorFacade workflowValidatorFacade = mock(WorkflowValidatorFacade.class);
    private final WorkflowValidatorTools workflowValidatorTools = new WorkflowValidatorTools(
        componentDefinitionService, workflowValidatorFacade);

    @Test
    void testValidateWorkflowReportsAComponentOutsideTheAllowList() {
        ComponentDefinition salesforceComponentDefinition = mock(ComponentDefinition.class);

        when(salesforceComponentDefinition.isConnectionRequired()).thenReturn(true);
        when(componentDefinitionService.fetchComponentDefinition(anyString(), isNull()))
            .thenReturn(Optional.empty());
        when(componentDefinitionService.fetchComponentDefinition("salesforce", null))
            .thenReturn(Optional.of(salesforceComponentDefinition));
        when(workflowValidatorFacade.validateWorkflow(WORKFLOW))
            .thenReturn(new WorkflowValidatorFacade.WorkflowValidationResult(List.of(), List.of()));

        WorkflowValidationResult workflowValidationResult = workflowValidatorTools.validateWorkflow(
            WORKFLOW, new ToolContext(
                Map.of(AllowedComponentNamesUtils.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, Set.of("slack"))));

        assertThat(workflowValidationResult.valid()).isFalse();
        assertThat(workflowValidationResult.errors())
            .contains("This chat can't use salesforce: it isn't one of the integrations available to this user.");
    }

    @Test
    void testValidateWorkflowWithoutTheAllowListIsUnaffected() {
        when(workflowValidatorFacade.validateWorkflow(WORKFLOW))
            .thenReturn(new WorkflowValidatorFacade.WorkflowValidationResult(List.of(), List.of()));

        WorkflowValidationResult workflowValidationResult = workflowValidatorTools.validateWorkflow(WORKFLOW, null);

        assertThat(workflowValidationResult.valid()).isTrue();
        verifyNoInteractions(componentDefinitionService);
    }
}
