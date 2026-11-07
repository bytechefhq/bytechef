/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.event;

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollectionEndpoint;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ApiCollectionWorkflowPreDeleteListener implements WorkflowPreDeleteListener {

    private static final Logger log = LoggerFactory.getLogger(ApiCollectionWorkflowPreDeleteListener.class);

    private final ApiCollectionEndpointService apiCollectionEndpointService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @SuppressFBWarnings("EI")
    public ApiCollectionWorkflowPreDeleteListener(
        ApiCollectionEndpointService apiCollectionEndpointService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService) {

        this.apiCollectionEndpointService = apiCollectionEndpointService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
    }

    @Override
    public void onWorkflowPreDelete(String workflowId) {
        log.debug("Cleaning up API collection data for workflow {}", workflowId);

        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
            projectDeploymentWorkflowService.getWorkflowProjectDeploymentWorkflows(workflowId);

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflows) {
            long projectDeploymentWorkflowId = projectDeploymentWorkflow.getId();

            List<ApiCollectionEndpoint> apiCollectionEndpoints =
                apiCollectionEndpointService.getProjectDeploymentWorkflowApiEndpoints(projectDeploymentWorkflowId);

            if (apiCollectionEndpoints.isEmpty()) {
                continue;
            }

            for (ApiCollectionEndpoint apiCollectionEndpoint : apiCollectionEndpoints) {
                apiCollectionEndpointService.delete(apiCollectionEndpoint.getId());
            }

            projectDeploymentWorkflowService.delete(projectDeploymentWorkflowId);
        }
    }
}
