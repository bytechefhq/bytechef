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
import com.bytechef.ee.embedded.configuration.repository.AutomationProjectRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AutomationProjectWorkflowOwnershipResolver implements ResourceOwnershipResolver {

    private final AutomationProjectRepository automationProjectRepository;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public AutomationProjectWorkflowOwnershipResolver(
        AutomationProjectRepository automationProjectRepository,
        ProjectWorkflowService projectWorkflowService) {

        this.automationProjectRepository = automationProjectRepository;
        this.projectWorkflowService = projectWorkflowService;
    }

    @Override
    public String resourceType() {
        return "AutomationProjectWorkflow";
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
            if (automationProjectRepository.findByProjectId(projectWorkflow.getProjectId())
                .isPresent()) {

                return ResourceOwner.ofEmbedded();
            }
        }

        return ResourceOwner.unknown();
    }
}
