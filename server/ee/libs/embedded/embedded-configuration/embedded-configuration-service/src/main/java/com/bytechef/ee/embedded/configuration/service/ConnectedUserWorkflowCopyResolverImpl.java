/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ConnectedUserWorkflowCopyResolver;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional(readOnly = true)
@ConditionalOnEEVersion
public class ConnectedUserWorkflowCopyResolverImpl implements ConnectedUserWorkflowCopyResolver {

    private final ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public ConnectedUserWorkflowCopyResolverImpl(
        ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService,
        ProjectWorkflowService projectWorkflowService) {

        this.connectedUserProjectWorkflowService = connectedUserProjectWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
    }

    @Override
    public List<String> getWorkflowCopyUuids(String workflowUuid) {
        Set<String> workflowCopyUuids = new LinkedHashSet<>();

        for (ConnectedUserProjectWorkflow connectedUserProjectWorkflow : connectedUserProjectWorkflowService
            .getCopiedConnectedUserProjectWorkflows(workflowUuid)) {

            Long projectWorkflowId = connectedUserProjectWorkflow.getProjectWorkflowId();

            if (projectWorkflowId == null) {
                continue;
            }

            ProjectWorkflow projectWorkflow = projectWorkflowService.getProjectWorkflow(projectWorkflowId);

            workflowCopyUuids.add(projectWorkflow.getUuidAsString());
        }

        return List.copyOf(workflowCopyUuids);
    }
}
