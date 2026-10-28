/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.event;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.automation.configuration.service.ProjectGitConfigurationService;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.data.relational.core.mapping.event.AbstractRelationalEventListener;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ProjectBeforeDeleteEventListener extends AbstractRelationalEventListener<Project> {

    private final CodeWorkflowContainerFacade codeWorkflowContainerFacade;
    private final ProjectCodeWorkflowService projectCodeWorkflowService;
    private final ProjectGitConfigurationService projectGitConfigurationService;

    @SuppressFBWarnings("EI")
    public ProjectBeforeDeleteEventListener(
        CodeWorkflowContainerFacade codeWorkflowContainerFacade, ProjectCodeWorkflowService projectCodeWorkflowService,
        ProjectGitConfigurationService projectGitConfigurationService) {

        this.codeWorkflowContainerFacade = codeWorkflowContainerFacade;
        this.projectCodeWorkflowService = projectCodeWorkflowService;
        this.projectGitConfigurationService = projectGitConfigurationService;
    }

    @Override
    protected void onBeforeDelete(BeforeDeleteEvent<Project> event) {
        Identifier identifier = event.getId();

        long projectId = (Long) identifier.getValue();

        projectGitConfigurationService.delete(projectId);

        List<ProjectCodeWorkflow> projectCodeWorkflows = projectCodeWorkflowService.getProjectCodeWorkflows(
            projectId);

        projectCodeWorkflowService.delete(projectId);

        for (ProjectCodeWorkflow projectCodeWorkflow : projectCodeWorkflows) {
            codeWorkflowContainerFacade.delete(projectCodeWorkflow.getCodeWorkflowContainerId());
        }
    }
}
