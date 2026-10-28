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
import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.automation.configuration.service.ProjectGitConfigurationService;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.relational.core.conversion.MutableAggregateChange;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectBeforeDeleteEventListenerTest {

    private static final long PROJECT_ID = 5L;

    private final CodeWorkflowContainerFacade codeWorkflowContainerFacade = mock(CodeWorkflowContainerFacade.class);
    private final ProjectCodeWorkflowService projectCodeWorkflowService = mock(ProjectCodeWorkflowService.class);
    private final ProjectGitConfigurationService projectGitConfigurationService =
        mock(ProjectGitConfigurationService.class);
    private final ProjectBeforeDeleteEventListener projectBeforeDeleteEventListener =
        new ProjectBeforeDeleteEventListener(
            codeWorkflowContainerFacade, projectCodeWorkflowService, projectGitConfigurationService);

    @Test
    void testOnBeforeDeleteDeletesTheGitConfiguration() {
        projectBeforeDeleteEventListener.onBeforeDelete(createBeforeDeleteEvent());

        verify(projectGitConfigurationService).delete(PROJECT_ID);
    }

    @Test
    void testOnBeforeDeleteDeletesTheCodeWorkflowsAndThenTheirContainers() {
        ProjectCodeWorkflow firstProjectCodeWorkflow = mock(ProjectCodeWorkflow.class);
        ProjectCodeWorkflow secondProjectCodeWorkflow = mock(ProjectCodeWorkflow.class);

        when(firstProjectCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(7L);
        when(secondProjectCodeWorkflow.getCodeWorkflowContainerId()).thenReturn(8L);
        when(projectCodeWorkflowService.getProjectCodeWorkflows(PROJECT_ID))
            .thenReturn(List.of(firstProjectCodeWorkflow, secondProjectCodeWorkflow));

        projectBeforeDeleteEventListener.onBeforeDelete(createBeforeDeleteEvent());

        InOrder inOrder = inOrder(projectCodeWorkflowService, codeWorkflowContainerFacade);

        inOrder.verify(projectCodeWorkflowService)
            .delete(PROJECT_ID);
        inOrder.verify(codeWorkflowContainerFacade)
            .delete(7L);
        inOrder.verify(codeWorkflowContainerFacade)
            .delete(8L);
    }

    private static BeforeDeleteEvent<Project> createBeforeDeleteEvent() {
        return new BeforeDeleteEvent<>(
            Identifier.of(PROJECT_ID), null, MutableAggregateChange.forDelete(Project.class));
    }
}
