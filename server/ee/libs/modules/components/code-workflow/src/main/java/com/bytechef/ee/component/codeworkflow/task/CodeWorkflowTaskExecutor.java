/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.codeworkflow.task;

import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.ee.component.codeworkflow.constant.CodeWorkflowConstants;
import com.bytechef.ee.embedded.codeworkflow.loader.IntegrationHandlerLoader;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.embedded.integration.IntegrationHandler;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.codeworkflow.loader.automation.ProjectHandlerLoader;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.workflow.definition.CompositeTaskDefinition;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.TaskDefinition.PerformFunction;
import com.bytechef.workflow.definition.WorkflowDefinition;
import com.bytechef.workflow.definition.WorkflowTaskDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class CodeWorkflowTaskExecutor {

    private final ActionDefinitionService actionDefinitionService;
    private final CacheManager cacheManager;
    private final CodeWorkflowContainerService codeWorkflowContainerService;
    private final CodeWorkflowFileStorage codeWorkflowFileStorage;
    private final ComponentDefinitionService componentDefinitionService;

    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public CodeWorkflowTaskExecutor(
        ActionDefinitionService actionDefinitionService, CacheManager cacheManager,
        CodeWorkflowContainerService codeWorkflowContainerService,
        CodeWorkflowFileStorage codeWorkflowFileStorage, ComponentDefinitionService componentDefinitionService) {

        this.actionDefinitionService = actionDefinitionService;
        this.cacheManager = cacheManager;
        this.codeWorkflowContainerService = codeWorkflowContainerService;
        this.codeWorkflowFileStorage = codeWorkflowFileStorage;
        this.componentDefinitionService = componentDefinitionService;
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    public Object executePerform(
        String codeWorkflowContainerUuid, String workflowName, String taskName, PlatformType type,
        Parameters inputParameters, Map<String, ? extends ComponentConnection> componentConnections,
        ActionContext actionContext) throws Exception {

        CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerService.getCodeWorkflowContainer(
            codeWorkflowContainerUuid);

        List<WorkflowDefinition> workflows = getWorkflowDefinitions(codeWorkflowContainer, type);

        WorkflowDefinition workflowDefinition = workflows.stream()
            .filter(workflow -> Objects.equals(workflow.getName(), workflowName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found"));

        TaskDefinition taskDefinition = findTaskDefinition(workflowDefinition.getTasks()
            .orElseThrow(), taskName);

        if (taskDefinition == null) {
            throw new IllegalArgumentException("Task name=%s not found".formatted(taskName));
        }

        PerformFunction performFunction = taskDefinition.getPerform();

        CodeWorkflowTaskContext taskContext = createTaskContext(
            componentConnections, actionContext, inputParameters.getMap(CodeWorkflowConstants.INPUT, Map.of()),
            inputParameters.getList(CodeWorkflowConstants.CONCURRENT_TASK_NAMES, String.class, List.of()),
            toDeclaredParameters(inputParameters));

        return performFunction.apply(taskContext);
    }

    private static TaskDefinition findTaskDefinition(
        List<? extends WorkflowTaskDefinition> tasks, String taskName) {

        for (WorkflowTaskDefinition task : tasks) {
            if (task instanceof CompositeTaskDefinition compositeTask) {
                TaskDefinition nestedTask = findTaskDefinition(compositeTask.getTasks(), taskName);

                if (nestedTask != null) {
                    return nestedTask;
                }

                for (List<? extends TaskDefinition> branch : compositeTask.getBranches()) {
                    TaskDefinition branchTask = findTaskDefinition(branch, taskName);

                    if (branchTask != null) {
                        return branchTask;
                    }
                }
            } else if (Objects.equals(task.getName(), taskName)) {
                return (TaskDefinition) task;
            }
        }

        return null;
    }

    CodeWorkflowTaskContext createTaskContext(
        Map<String, ? extends ComponentConnection> componentConnections, ActionContext actionContext,
        Map<String, ?> input, List<String> concurrentTaskNames, Map<String, ?> parameters) {

        return new CodeWorkflowTaskContext(
            actionContext, actionDefinitionService, componentConnections, componentDefinitionService, input,
            concurrentTaskNames, parameters);
    }

    private static Map<String, ?> toDeclaredParameters(Parameters inputParameters) {
        Map<String, ?> parameters = new LinkedHashMap<>(inputParameters.toMap());

        parameters.keySet()
            .removeAll(CodeWorkflowConstants.DISPATCH_PARAMETERS);

        return parameters;
    }

    private List<WorkflowDefinition> getWorkflowDefinitions(
        CodeWorkflowContainer codeWorkflowContainer, PlatformType type) {

        List<WorkflowDefinition> workflows = List.of();

        if (PlatformType.AUTOMATION.equals(type)) {
            ProjectHandler projectHandler = ProjectHandlerLoader.loadProjectHandler(
                codeWorkflowFileStorage.getCodeWorkflowFileURL(codeWorkflowContainer.getWorkflows()),
                codeWorkflowContainer.getLanguage(),
                EncodingUtils.base64EncodeToString(codeWorkflowContainer.toString()), cacheManager);

            workflows = projectHandler.getWorkflows();
        } else if (PlatformType.EMBEDDED.equals(type)) {
            IntegrationHandler integrationHandler = IntegrationHandlerLoader.loadIntegrationHandler(
                codeWorkflowFileStorage.getCodeWorkflowFileURL(codeWorkflowContainer.getWorkflows()),
                codeWorkflowContainer.getLanguage(),
                EncodingUtils.base64EncodeToString(codeWorkflowContainer.toString()), cacheManager);

            workflows = integrationHandler.getWorkflows();
        }

        return workflows;
    }
}
