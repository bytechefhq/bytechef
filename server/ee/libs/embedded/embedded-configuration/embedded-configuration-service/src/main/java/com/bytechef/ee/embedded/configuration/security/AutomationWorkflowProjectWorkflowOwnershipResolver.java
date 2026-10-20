/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.repository.AutomationWorkflowProjectRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Keyed by the project workflow uuid, the version-independent identifier shared by every version of a workflow. Reads
 * repositories and the unguarded {@link ProjectWorkflowService} only, because it runs inside permission checks. Answers
 * only when one of the workflow's projects is a Hub project.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AutomationWorkflowProjectWorkflowOwnershipResolver implements ResourceOwnershipResolver {

    private final AutomationWorkflowProjectRepository automationWorkflowProjectRepository;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectWorkflowOwnershipResolver(
        AutomationWorkflowProjectRepository automationWorkflowProjectRepository,
        ProjectWorkflowService projectWorkflowService) {

        this.automationWorkflowProjectRepository = automationWorkflowProjectRepository;
        this.projectWorkflowService = projectWorkflowService;
    }

    @Override
    public String resourceType() {
        return "AutomationWorkflowProjectWorkflow";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return ResourceOwner.unknown();
    }

    @Override
    public ResourceOwner resolveOwner(Serializable id) {
        if (!(id instanceof String) && !(id instanceof UUID)) {
            return ResourceOwner.unknown();
        }

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getWorkflowUuidProjectWorkflows(
            id.toString());

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            if (automationWorkflowProjectRepository.findByProjectId(projectWorkflow.getProjectId())
                .isPresent()) {

                return ResourceOwner.ofEmbedded();
            }
        }

        return ResourceOwner.unknown();
    }
}
