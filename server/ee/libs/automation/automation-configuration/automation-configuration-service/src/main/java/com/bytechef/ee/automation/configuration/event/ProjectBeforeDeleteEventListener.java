/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.event;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.automation.configuration.service.ProjectGitConfigurationService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

    private final ProjectCodeWorkflowService projectCodeWorkflowService;
    private final ProjectGitConfigurationService projectGitConfigurationService;
    private final ResourceGrantService resourceGrantService;

    @SuppressFBWarnings("EI")
    public ProjectBeforeDeleteEventListener(
        ProjectCodeWorkflowService projectCodeWorkflowService,
        ProjectGitConfigurationService projectGitConfigurationService,
        ResourceGrantService resourceGrantService) {

        this.projectCodeWorkflowService = projectCodeWorkflowService;
        this.projectGitConfigurationService = projectGitConfigurationService;
        this.resourceGrantService = resourceGrantService;
    }

    @Override
    protected void onBeforeDelete(BeforeDeleteEvent<Project> event) {
        Identifier identifier = event.getId();

        long projectId = (Long) identifier.getValue();

        // Grants first: resource_grant.resource_id is polymorphic and carries no foreign key, so a grant left behind
        // would attach to whatever later recycles this id.
        resourceGrantService.deleteGrants(ProjectVisibilityFilter.PROJECT, projectId);

        projectCodeWorkflowService.deleteProjectCodeWorkflows(projectId);
        projectGitConfigurationService.delete(projectId);
    }
}
