/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.security.EmbeddedResourceResolver;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.repository.AutomationWorkflowProjectRepository;
import com.bytechef.ee.embedded.configuration.repository.ConnectUserProjectRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationWorkflowRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Reads repositories and the unguarded {@link ProjectWorkflowService} only, because it runs inside permission checks.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class EmbeddedResourceResolverImpl implements EmbeddedResourceResolver {

    private final AutomationWorkflowProjectRepository automationWorkflowProjectRepository;
    private final ConnectUserProjectRepository connectUserProjectRepository;
    private final IntegrationWorkflowRepository integrationWorkflowRepository;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public EmbeddedResourceResolverImpl(
        AutomationWorkflowProjectRepository automationWorkflowProjectRepository,
        ConnectUserProjectRepository connectUserProjectRepository,
        IntegrationWorkflowRepository integrationWorkflowRepository, ProjectWorkflowService projectWorkflowService) {

        this.automationWorkflowProjectRepository = automationWorkflowProjectRepository;
        this.connectUserProjectRepository = connectUserProjectRepository;
        this.integrationWorkflowRepository = integrationWorkflowRepository;
        this.projectWorkflowService = projectWorkflowService;
    }

    @Override
    public boolean isEmbeddedProject(long projectId) {
        if (automationWorkflowProjectRepository.findByProjectId(projectId)
            .isPresent()) {

            return true;
        }

        return connectUserProjectRepository.existsByProjectId(projectId);
    }

    @Override
    public boolean isEmbeddedWorkflow(String workflowId) {
        if (integrationWorkflowRepository.findByWorkflowId(workflowId)
            .isPresent()) {

            return true;
        }

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getWorkflowProjectWorkflows(
            List.of(workflowId));

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            if (isEmbeddedProject(projectWorkflow.getProjectId())) {
                return true;
            }
        }

        return false;
    }
}
