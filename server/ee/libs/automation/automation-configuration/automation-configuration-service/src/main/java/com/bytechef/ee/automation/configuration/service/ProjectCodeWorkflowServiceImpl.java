/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.ee.automation.configuration.repository.ProjectCodeWorkflowRepository;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class ProjectCodeWorkflowServiceImpl implements ProjectCodeWorkflowService {

    private final ProjectCodeWorkflowRepository projectCodeWorkflowRepository;

    public ProjectCodeWorkflowServiceImpl(ProjectCodeWorkflowRepository projectCodeWorkflowRepository) {
        this.projectCodeWorkflowRepository = projectCodeWorkflowRepository;
    }

    @Override
    public ProjectCodeWorkflow create(CodeWorkflowContainer codeWorkflowContainer, Project project) {
        ProjectCodeWorkflow projectCodeWorkflow = new ProjectCodeWorkflow();

        projectCodeWorkflow.setCodeWorkflowContainer(codeWorkflowContainer);
        projectCodeWorkflow.setProject(project);
        projectCodeWorkflow.setProjectVersion(project.getLastVersion());

        return projectCodeWorkflowRepository.save(projectCodeWorkflow);
    }
}
