/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.codeworkflow.task;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
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

        // } else {TODO integration}

        return workflows;
    }
}
