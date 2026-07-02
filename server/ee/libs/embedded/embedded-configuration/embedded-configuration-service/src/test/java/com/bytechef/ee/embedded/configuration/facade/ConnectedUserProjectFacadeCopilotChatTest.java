/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.CopilotChatContextDTO;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserProjectFacadeCopilotChatTest {

    @Test
    void testPrepareCopilotChatResolvesWorkflowIdAndAllowedComponents() {
        ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);

        when(projectWorkflowService.getLastWorkflowId(eq("uuid-1"))).thenReturn("wf-99");

        ConnectedUserProjectFacadeImpl facade = buildFacadeSpy(projectWorkflowService, Set.of("slack", "gmail"));

        CopilotChatContextDTO context = facade.prepareCopilotChat("ext-1", "uuid-1", Environment.PRODUCTION);

        assertThat(context.workflowId()).isEqualTo("wf-99");
        assertThat(context.allowedComponentNames()).containsExactlyInAnyOrder("slack", "gmail");
    }

    /**
     * Constructs a {@link ConnectedUserProjectFacadeImpl} spy with only the collaborators required by
     * {@code prepareCopilotChat} wired. The {@code resolveAllowedComponentNames} and
     * {@code getConnectedUserProjectWorkflow} methods are stubbed so the test does not need a Spring context.
     */
    private static ConnectedUserProjectFacadeImpl buildFacadeSpy(
        ProjectWorkflowService projectWorkflowService, Set<String> allowedComponentNames) {

        ConnectedUserProjectFacadeImpl impl = new ConnectedUserProjectFacadeImpl(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, projectWorkflowService, null, null, null, null);

        ConnectedUserProjectFacadeImpl facadeSpy = spy(impl);

        ConnectedUserProjectWorkflowDTO stubWorkflow = mock(ConnectedUserProjectWorkflowDTO.class);

        doReturn(stubWorkflow)
            .when(facadeSpy)
            .getConnectedUserProjectWorkflow(eq("ext-1"), eq("uuid-1"), eq((long) Environment.PRODUCTION.ordinal()));

        doReturn(allowedComponentNames)
            .when(facadeSpy)
            .resolveAllowedComponentNames(eq(Environment.PRODUCTION));

        return facadeSpy;
    }
}
