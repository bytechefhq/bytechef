/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.codeworkflow.task;

import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Workflow.CodeWorkflow;
import com.bytechef.ee.embedded.codeworkflow.loader.IntegrationHandlerLoader;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.embedded.integration.IntegrationHandler;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.codeworkflow.loader.automation.ProjectHandlerLoader;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.workflow.definition.TaskDefinition.PerformFunction;
import com.bytechef.workflow.definition.WorkflowDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
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

    private final CacheManager cacheManager;
    private final CodeWorkflowFileStorage codeWorkflowFileStorage;
    private final CodeWorkflowContainerService codeWorkflowContainerService;
    private final CodeWorkflow.JavaLoader javaLoader;

    @SuppressFBWarnings("EI")
    public CodeWorkflowTaskExecutor(
        ApplicationProperties applicationProperties, CacheManager cacheManager,
        CodeWorkflowFileStorage codeWorkflowFileStorage, CodeWorkflowContainerService codeWorkflowContainerService) {

        this.cacheManager = cacheManager;
        this.codeWorkflowFileStorage = codeWorkflowFileStorage;
        this.codeWorkflowContainerService = codeWorkflowContainerService;

        ApplicationProperties.Workflow workflow = applicationProperties.getWorkflow();

        CodeWorkflow codeWorkflow = workflow.getCodeWorkflow();

        this.javaLoader = codeWorkflow.getJavaLoader();
    }

    public Object executePerform(
        String codeWorkflowContainerUuid, String workflowName, String taskName, PlatformType type) {

        CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerService.getCodeWorkflowContainer(
            codeWorkflowContainerUuid);

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

    private List<WorkflowDefinition> getWorkflowDefinitions(
        CodeWorkflowContainer codeWorkflowContainer, PlatformType type) {

        List<WorkflowDefinition> workflows = List.of();

        if (PlatformType.AUTOMATION.equals(type)) {
            ProjectHandler projectHandler = ProjectHandlerLoader.loadProjectHandler(
                codeWorkflowFileStorage.getCodeWorkflowFileURL(codeWorkflowContainer.getWorkflows()),
                codeWorkflowContainer.getLanguage(),
                javaLoader == CodeWorkflow.JavaLoader.ESPRESSO
                    ? ProjectHandlerLoader.JavaLoader.ESPRESSO
                    : ProjectHandlerLoader.JavaLoader.CLASS_LOADER,
                EncodingUtils.base64EncodeToString(codeWorkflowContainer.toString()), cacheManager);

            workflows = projectHandler.getWorkflows();
        } else if (PlatformType.EMBEDDED.equals(type)) {
            IntegrationHandler integrationHandler = IntegrationHandlerLoader.loadIntegrationHandler(
                codeWorkflowFileStorage.getCodeWorkflowFileURL(codeWorkflowContainer.getWorkflows()),
                codeWorkflowContainer.getLanguage(),
                javaLoader == CodeWorkflow.JavaLoader.ESPRESSO
                    ? IntegrationHandlerLoader.JavaLoader.ESPRESSO
                    : IntegrationHandlerLoader.JavaLoader.CLASS_LOADER,
                EncodingUtils.base64EncodeToString(codeWorkflowContainer.toString()), cacheManager);

            workflows = integrationHandler.getWorkflows();
        }

        return workflows;
    }
}
