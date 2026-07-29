/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.service.ProjectCodeWorkflowInfoSupplier;
import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Enterprise implementation of the {@link ProjectCodeWorkflowInfoSupplier} SPI. Resolves the latest code workflow
 * container for a project and exposes its language. Returns {@link Optional#empty()} when the project has no code
 * workflow.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ProjectCodeWorkflowInfoSupplierImpl implements ProjectCodeWorkflowInfoSupplier {

    private final CodeWorkflowContainerService codeWorkflowContainerService;
    private final ProjectCodeWorkflowService projectCodeWorkflowService;

    public ProjectCodeWorkflowInfoSupplierImpl(
        CodeWorkflowContainerService codeWorkflowContainerService,
        ProjectCodeWorkflowService projectCodeWorkflowService) {

        this.codeWorkflowContainerService = codeWorkflowContainerService;
        this.projectCodeWorkflowService = projectCodeWorkflowService;
    }

    @Override
    public Optional<CodeWorkflowInfo> fetchCodeWorkflowInfo(long projectId) {
        ProjectCodeWorkflow projectCodeWorkflow;

        try {
            projectCodeWorkflow = projectCodeWorkflowService.getProjectCodeWorkflow(projectId);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerService.getCodeWorkflowContainer(
            projectCodeWorkflow.getCodeWorkflowContainerId());

        return Optional.of(new CodeWorkflowInfo(codeWorkflowContainer.getLanguage()
            .name()));
    }
}
