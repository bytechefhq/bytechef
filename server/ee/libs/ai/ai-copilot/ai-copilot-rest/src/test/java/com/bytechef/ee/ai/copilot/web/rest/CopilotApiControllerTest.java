/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.server.LocalAgent;
import com.agui.server.spring.AgUiParameters;
import com.agui.server.spring.AgUiService;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.ai.copilot.web.rest.facade.CopilotChatFacade;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Pins that the chat run reaches the AG-UI service through the facade layer, which is where this codebase puts
 * authorization. The mode-dependent workflow-scope guard itself is pinned by {@code CopilotChatFacadeImplTest}.
 *
 * <p>
 * The controller carries no gate of its own and is not meant to. Asserting the delegation is not enough on its own:
 * {@code testControllerHoldsNoRunOrAuthorizationCollaborators} is what makes a revert to a locally written
 * {@code permissionService} check — or to dispatching the agent from here, past the facade — fail rather than pass,
 * since either has to reintroduce one of those collaborators as a field.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class CopilotApiControllerTest {

    private final CopilotChatFacade copilotChatFacade = mock(CopilotChatFacade.class);

    private final CopilotApiController controller = new CopilotApiController(copilotChatFacade);

    @Test
    void testChatDelegatesToTheGuardedFacade() {
        AgUiParameters parameters = mock(AgUiParameters.class);
        SseEmitter sseEmitter = new SseEmitter();

        when(copilotChatFacade.chat("workflow_editor", parameters)).thenReturn(sseEmitter);

        assertThat(controller.chat("workflow_editor", parameters)).isSameAs(sseEmitter);

        verify(copilotChatFacade).chat("workflow_editor", parameters);
    }

    @Test
    void testControllerHoldsNoRunOrAuthorizationCollaborators() {
        assertThat(Arrays.stream(CopilotApiController.class.getDeclaredFields())
            .map(Field::getType))
                .as("authorization and agent dispatch belong on the facade, not on the controller")
                .doesNotContain(
                    PermissionService.class, ProjectWorkflowService.class, AgUiService.class, LocalAgent.class);
    }
}
