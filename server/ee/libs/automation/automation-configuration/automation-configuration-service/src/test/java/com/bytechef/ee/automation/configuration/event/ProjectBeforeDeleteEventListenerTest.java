/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.event;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.automation.configuration.service.ProjectGitConfigurationService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;

/**
 * Pins that deleting a project purges its named-user grants — spec §13 claimed this was tested and it was not, while
 * the connection equivalent ({@code ConnectionSharingFacadeTest}) was, which made the asymmetry read as coverage that
 * did not exist.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectBeforeDeleteEventListenerTest {

    private static final long PROJECT_ID = 42L;

    private final ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);
    private final ProjectGitConfigurationService projectGitConfigurationService =
        mock(ProjectGitConfigurationService.class);
    private final ResourceGrantService resourceGrantService = mock(ResourceGrantService.class);

    private final ProjectBeforeDeleteEventListener projectBeforeDeleteEventListener =
        new ProjectBeforeDeleteEventListener(
            projectCodeWorkflowService, projectGitConfigurationService, resourceGrantService);

    /**
     * {@code resource_grant.resource_id} is polymorphic and carries no foreign key, so nothing in the database removes
     * these rows for us: a grant left behind would attach to whatever project later recycles this id.
     */
    @Test
    void testDeletingAProjectDeletesItsGrants() {
        projectBeforeDeleteEventListener.onBeforeDelete(event());

        // "Project" is the type the grants were written under -- ProjectVisibilityFilter.PROJECT, the same token
        // every inheriting child's provider redirects to. A different token here would leave every grant behind.
        verify(resourceGrantService).deleteGrants("Project", PROJECT_ID);
    }

    /**
     * Grants go first, ahead of the code-workflow and git cleanups. Either of those can throw, and this listener runs
     * before the delete rather than after it, so an ordering flip would leave the grants behind on exactly the failures
     * that also abandon the row.
     */
    @Test
    void testGrantsArePurgedBeforeTheOtherCleanups() {
        projectBeforeDeleteEventListener.onBeforeDelete(event());

        InOrder inOrder = inOrder(resourceGrantService, projectCodeWorkflowService, projectGitConfigurationService);

        inOrder.verify(resourceGrantService)
            .deleteGrants("Project", PROJECT_ID);
        inOrder.verify(projectCodeWorkflowService)
            .deleteProjectCodeWorkflows(PROJECT_ID);
        inOrder.verify(projectGitConfigurationService)
            .delete(PROJECT_ID);
    }

    @SuppressWarnings("unchecked")
    private static BeforeDeleteEvent<Project> event() {
        BeforeDeleteEvent<Project> beforeDeleteEvent = mock(BeforeDeleteEvent.class);
        Identifier identifier = mock(Identifier.class);

        when(beforeDeleteEvent.getId()).thenReturn(identifier);
        when(identifier.getValue()).thenReturn(PROJECT_ID);

        return beforeDeleteEvent;
    }
}
