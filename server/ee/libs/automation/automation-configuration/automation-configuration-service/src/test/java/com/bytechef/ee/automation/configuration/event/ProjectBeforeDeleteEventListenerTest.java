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
import com.bytechef.ee.automation.configuration.service.ProjectGitConfigurationService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectBeforeDeleteEventListenerTest {

    private static final long PROJECT_ID = 42L;

    private final ProjectGitConfigurationService projectGitConfigurationService =
        mock(ProjectGitConfigurationService.class);
    private final ResourceGrantService resourceGrantService = mock(ResourceGrantService.class);

    private final ProjectBeforeDeleteEventListener projectBeforeDeleteEventListener =
        new ProjectBeforeDeleteEventListener(projectGitConfigurationService, resourceGrantService);

    @Test
    void testDeletingAProjectDeletesItsGrants() {
        projectBeforeDeleteEventListener.onBeforeDelete(event());

        verify(resourceGrantService).deleteGrants("Project", PROJECT_ID);
    }

    @Test
    void testGrantsArePurgedBeforeTheOtherCleanups() {
        projectBeforeDeleteEventListener.onBeforeDelete(event());

        InOrder inOrder = inOrder(resourceGrantService, projectGitConfigurationService);

        inOrder.verify(resourceGrantService)
            .deleteGrants("Project", PROJECT_ID);
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
