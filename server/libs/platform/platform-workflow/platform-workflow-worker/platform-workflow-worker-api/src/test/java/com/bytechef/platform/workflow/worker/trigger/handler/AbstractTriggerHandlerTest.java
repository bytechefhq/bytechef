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

package com.bytechef.platform.workflow.worker.trigger.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AbstractTriggerHandlerTest {

    @Test
    void testHandleForwardsTheTriggerExecutionIdSoItsLogsCanBeStored() throws Exception {
        TriggerDefinitionFacade triggerDefinitionFacade = mock(TriggerDefinitionFacade.class);
        WorkflowExecutionId workflowExecutionId = mock(WorkflowExecutionId.class);
        TriggerExecution triggerExecution = mock(TriggerExecution.class);

        when(workflowExecutionId.getJobPrincipalId()).thenReturn(5L);
        when(workflowExecutionId.getWorkflowUuid()).thenReturn("workflow-uuid");
        when(workflowExecutionId.getType()).thenReturn(PlatformType.AUTOMATION);
        when(triggerExecution.getId()).thenReturn(77L);
        when(triggerExecution.getWorkflowExecutionId()).thenReturn(workflowExecutionId);
        when(triggerExecution.getMetadata()).thenReturn(Map.of());
        when(triggerExecution.getParameters()).thenReturn(Map.of());

        AbstractTriggerHandler triggerHandler = new AbstractTriggerHandler(
            "webhook", 1, "newRequest", triggerDefinitionFacade) {};

        triggerHandler.handle(triggerExecution);

        verify(triggerDefinitionFacade).executeTrigger(
            eq("webhook"), eq(1), eq("newRequest"), eq(5L), eq("workflow-uuid"), eq(77L), anyMap(), any(), any(),
            any(), any(), eq(PlatformType.AUTOMATION), anyBoolean());
    }
}
