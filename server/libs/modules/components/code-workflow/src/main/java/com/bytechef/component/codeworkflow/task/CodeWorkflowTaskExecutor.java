/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.codeworkflow.task;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.platform.codeworkflow.loader.automation.ProjectHandlerLoader;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.workflow.ProjectHandler;
import com.bytechef.workflow.definition.TaskDefinition.PerformFunction;
import com.bytechef.workflow.definition.WorkflowDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class CodeWorkflowTaskExecutor {

    private final CodeWorkflowFileStorage codeWorkflowFileStorage;
    private final CodeWorkflowContainerService codeWorkflowContainerService;

    @SuppressFBWarnings("EI")
    public CodeWorkflowTaskExecutor(
        CodeWorkflowFileStorage codeWorkflowFileStorage, CodeWorkflowContainerService codeWorkflowContainerService) {

        this.codeWorkflowFileStorage = codeWorkflowFileStorage;
        this.codeWorkflowContainerService = codeWorkflowContainerService;
    }

    public Object executePerform(
        String codeWorkflowContainerReference, String workflowName, String taskName, ModeType type) {

        CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerService.getCodeWorkflowContainer(
            codeWorkflowContainerReference);

        List<WorkflowDefinition> workflows = getWorkflowDefinitions(codeWorkflowContainer, type);

        WorkflowDefinition workflowDefinition = workflows.stream()
            .filter(workflow -> Objects.equals(workflow.getName(), workflowName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found"));

        PerformFunction performFunction = workflowDefinition.getTasks()
            .orElseThrow()
            .stream()
            .filter(task -> Objects.equals(task.getName(), taskName))
            .findFirst()
            .orElseThrow()
            .getPerform();

        return performFunction.apply();
    }

    private List<WorkflowDefinition>
        getWorkflowDefinitions(CodeWorkflowContainer codeWorkflowContainer, ModeType type) {
        List<WorkflowDefinition> workflows = List.of();

        if (ModeType.AUTOMATION.equals(type)) {
            ProjectHandler projectHandler = ProjectHandlerLoader.loadProjectHandler(
                codeWorkflowFileStorage.getCodeWorkflowFileURL(codeWorkflowContainer.getWorkflowsFile()),
                codeWorkflowContainer.getLanguage(),
                EncodingUtils.base64EncodeToString(codeWorkflowContainer.toString()));

            workflows = projectHandler.getWorkflows();
        }

        // } else {TODO embedded}

        return workflows;
    }
}
