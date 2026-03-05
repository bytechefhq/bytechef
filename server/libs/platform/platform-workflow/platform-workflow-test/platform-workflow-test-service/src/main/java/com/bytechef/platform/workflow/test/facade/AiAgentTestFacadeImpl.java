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

package com.bytechef.platform.workflow.test.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.workflow.test.util.TestAttachmentUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class AiAgentTestFacadeImpl implements AiAgentTestFacade {

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final Evaluator evaluator;
    private final TempFileStorage tempFileStorage;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public AiAgentTestFacadeImpl(
        ActionDefinitionFacade actionDefinitionFacade, Evaluator evaluator, TempFileStorage tempFileStorage,
        WorkflowNodeOutputFacade workflowNodeOutputFacade, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.evaluator = evaluator;
        this.tempFileStorage = tempFileStorage;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object executeAiAgentAction(
        String workflowId, String workflowNodeName, long environmentId, String conversationId, String message,
        List<Object> attachments) {

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTask workflowTask = workflow.getTasks(true)
            .stream()
            .filter(task -> Objects.equals(task.getName(), workflowNodeName))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "Workflow task not found: %s".formatted(workflowNodeName)));

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        Map<String, Object> taskParameters = new HashMap<>(workflowTask.getParameters());

        taskParameters.put("conversationId", conversationId);
        taskParameters.put("userPrompt", message);
        taskParameters.put("attachments", attachments);

        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflowId, environmentId);

        Map<String, ?> outputs = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
            workflowId, workflowNodeName, environmentId);

        Map<String, Object> evaluatedParameters = evaluator.evaluate(
            taskParameters, MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs));

        if (evaluatedParameters.containsKey("attachments")) {
            evaluatedParameters.put(
                "attachments", TestAttachmentUtils.getFileEntries(tempFileStorage, evaluatedParameters));
        }

        List<WorkflowTestConfigurationConnection> workflowTestConfigurationConnections =
            workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
                workflowId, workflowNodeName, environmentId);

        Map<String, Long> connectionIds = MapUtils.toMap(
            workflowTestConfigurationConnections, WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
            WorkflowTestConfigurationConnection::getConnectionId);

        Map<String, ?> extensions = workflowTask.getExtensions();

        Map<String, Object> evaluatedExtensions = evaluator.evaluate(
            extensions, MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs));

        return actionDefinitionFacade.executePerform(
            workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation(), null, null, null,
            null, workflowId, evaluatedParameters, connectionIds, evaluatedExtensions, environmentId, null,
            true, null, null);
    }
}
