/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.configuration.facade.ProjectSharingFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for project visibility and sharing.
 *
 * <p>
 * Authorization (owner-or-admin) and every validation are enforced on {@link ProjectSharingFacade}, not here, so they
 * protect every caller of the facade rather than this entry point alone. This class only maps arguments.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class ProjectSharingGraphQlController {

    private final ProjectSharingFacade projectSharingFacade;

    @SuppressFBWarnings("EI")
    public ProjectSharingGraphQlController(ProjectSharingFacade projectSharingFacade) {
        this.projectSharingFacade = projectSharingFacade;
    }

    @QueryMapping
    public List<Long> projectGrants(@Argument long workspaceId, @Argument long projectId) {
        return projectSharingFacade.getProjectGrants(workspaceId, projectId);
    }

    @MutationMapping
    public boolean setProjectVisibility(
        @Argument long workspaceId, @Argument long projectId, @Argument ResourceVisibility visibility) {

        projectSharingFacade.setProjectVisibility(workspaceId, projectId, visibility);

        return true;
    }

    @MutationMapping
    public boolean grantProjectAccess(@Argument long workspaceId, @Argument long projectId, @Argument long userId) {
        projectSharingFacade.grantProjectAccess(workspaceId, projectId, userId);

        return true;
    }

    @MutationMapping
    public boolean revokeProjectAccess(@Argument long workspaceId, @Argument long projectId, @Argument long userId) {
        projectSharingFacade.revokeProjectAccess(workspaceId, projectId, userId);

        return true;
    }
}
