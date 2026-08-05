/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.service.ProjectCodeWorkflowInfoSupplier;
import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.ee.automation.configuration.facade.ProjectCodeWorkflowFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final CodeWorkflowFileStorage codeWorkflowFileStorage;
    private final ProjectCodeWorkflowFacade projectCodeWorkflowFacade;
    private final ProjectCodeWorkflowService projectCodeWorkflowService;

    @SuppressFBWarnings("EI")
    public ProjectCodeWorkflowInfoSupplierImpl(
        CodeWorkflowContainerService codeWorkflowContainerService, CodeWorkflowFileStorage codeWorkflowFileStorage,
        ProjectCodeWorkflowFacade projectCodeWorkflowFacade,
        ProjectCodeWorkflowService projectCodeWorkflowService) {

        this.codeWorkflowContainerService = codeWorkflowContainerService;
        this.codeWorkflowFileStorage = codeWorkflowFileStorage;
        this.projectCodeWorkflowFacade = projectCodeWorkflowFacade;
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

    @Override
    public List<Long> getCodeWorkflowProjectIds() {
        return projectCodeWorkflowService.getCodeWorkflowProjectIds();
    }

    @Override
    public Optional<CodeWorkflowSource> fetchCodeWorkflowSource(long projectId) {
        ProjectCodeWorkflow projectCodeWorkflow;

        try {
            projectCodeWorkflow = projectCodeWorkflowService.getProjectCodeWorkflow(projectId);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerService.getCodeWorkflowContainer(
            projectCodeWorkflow.getCodeWorkflowContainerId());

        Language language = codeWorkflowContainer.getLanguage();

        // A Java container holds a compiled jar, which a duplicate or an export cannot carry as text.
        if (language == Language.JAVA) {
            return Optional.empty();
        }

        return Optional.of(
            new CodeWorkflowSource(
                language.name(),
                codeWorkflowFileStorage.readCodeWorkflowFileContent(codeWorkflowContainer.getWorkflows())));
    }

    @Override
    public void deployCodeWorkflowSource(long projectId, String language, String source) {
        projectCodeWorkflowFacade.deployCodeWorkflowSource(projectId, Language.valueOf(language), source);
    }

    @Override
    public Map<Long, String> getCodeWorkflowLanguages() {
        Map<Long, String> languages = new HashMap<>();

        for (Long projectId : projectCodeWorkflowService.getCodeWorkflowProjectIds()) {
            fetchCodeWorkflowInfo(projectId)
                .ifPresent(codeWorkflowInfo -> languages.put(projectId, codeWorkflowInfo.language()));
        }

        return languages;
    }
}
