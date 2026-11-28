/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.copilot;

import com.bytechef.ai.copilot.security.CopilotEmbeddedAccessChecker;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.embedded.configuration.facade.AutomationProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.ee.embedded.connected.user.security.ConnectedUserAuthentication;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class EmbeddedCopilotAccessChecker implements CopilotEmbeddedAccessChecker {

    private final AutomationProjectFacade automationProjectFacade;
    private final IntegrationWorkflowFacade integrationWorkflowFacade;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public EmbeddedCopilotAccessChecker(
        AutomationProjectFacade automationProjectFacade, IntegrationWorkflowFacade integrationWorkflowFacade,
        ProjectService projectService) {

        this.automationProjectFacade = automationProjectFacade;
        this.integrationWorkflowFacade = integrationWorkflowFacade;
        this.projectService = projectService;
    }

    @Override
    public AutomationProjectScope checkAutomationProjectWorkflowAccess(String workflowId) {
        long projectId = automationProjectFacade.getProjectWorkflowProjectId(workflowId);

        Project project = projectService.getProject(projectId);

        return new AutomationProjectScope(projectId, project.getWorkspaceId());
    }

    @Override
    public void checkIntegrationWorkflowAccess(String workflowId) {
        integrationWorkflowFacade.getIntegrationWorkflow(workflowId);
    }

    @Override
    public boolean isConnectedUser(Authentication authentication) {
        return authentication instanceof ConnectedUserAuthentication;
    }
}
