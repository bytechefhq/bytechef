/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProject;
import com.bytechef.ee.embedded.configuration.repository.AutomationWorkflowProjectRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AutomationWorkflowProjectWorkflowOwnershipResolverTest {

    private static final String WORKFLOW_UUID = UUID.randomUUID()
        .toString();

    private final AutomationWorkflowProjectRepository automationWorkflowProjectRepository =
        mock(AutomationWorkflowProjectRepository.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final AutomationWorkflowProjectWorkflowOwnershipResolver resolver =
        new AutomationWorkflowProjectWorkflowOwnershipResolver(
            automationWorkflowProjectRepository, projectWorkflowService);

    @Test
    void testResourceTypeMatchesTheGateToken() {
        assertThat(resolver.resourceType()).isEqualTo("AutomationWorkflowProjectWorkflow");
    }

    @Test
    void testResolvesAnEmbeddedRow() {
        when(projectWorkflowService.getWorkflowUuidProjectWorkflows(WORKFLOW_UUID))
            .thenReturn(List.of(new ProjectWorkflow(7L, 1, "workflow-1")));
        when(automationWorkflowProjectRepository.findByProjectId(7L))
            .thenReturn(Optional.of(new AutomationWorkflowProject(7L)));

        assertThat(resolver.resolveOwner(WORKFLOW_UUID)).isEqualTo(ResourceOwner.ofEmbedded());
        assertThat(resolver.resolveOwner(UUID.fromString(WORKFLOW_UUID))).isEqualTo(ResourceOwner.ofEmbedded());
    }

    @Test
    void testAMissingRowIsUnknown() {
        when(projectWorkflowService.getWorkflowUuidProjectWorkflows(WORKFLOW_UUID)).thenReturn(List.of());

        assertThat(resolver.resolveOwner(WORKFLOW_UUID)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testAnotherSidesRowIsUnknown() {
        when(projectWorkflowService.getWorkflowUuidProjectWorkflows(WORKFLOW_UUID))
            .thenReturn(List.of(new ProjectWorkflow(7L, 1, "workflow-1")));
        when(automationWorkflowProjectRepository.findByProjectId(7L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(WORKFLOW_UUID)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testANumericIdIsUnknown() {
        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.unknown());
        assertThat(resolver.resolveOwner(Long.valueOf(7L))).isEqualTo(ResourceOwner.unknown());
    }
}
