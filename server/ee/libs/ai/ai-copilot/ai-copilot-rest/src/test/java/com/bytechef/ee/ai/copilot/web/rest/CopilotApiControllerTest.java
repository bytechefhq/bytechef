/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.agui.server.spring.AgUiParameters;
import com.agui.server.spring.AgUiService;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentication;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CopilotApiControllerTest {

    private static final long FORGED_USER_ID = 1L;
    private static final String PLATFORM_LOGIN = "user@localhost.com";
    private static final long PLATFORM_USER_ID = 5L;

    private final AgUiService agUiService = mock(AgUiService.class);
    private final LocalAgent localAgent = mock(LocalAgent.class);
    private final UserService userService = mock(UserService.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testConnectedUserIsDenied() {
        Authentication connectedUser = mock(
            Authentication.class, withSettings().extraInterfaces(ConnectedUserAuthentication.class));

        when(connectedUser.getName()).thenReturn(PLATFORM_LOGIN);
        when(connectedUser.getPrincipal()).thenReturn(PLATFORM_LOGIN);
        when(connectedUser.isAuthenticated()).thenReturn(true);

        setAuthentication(connectedUser);

        CopilotApiController copilotApiController = newCopilotApiController();
        AgUiParameters agUiParameters = newAgUiParameters();

        assertThatThrownBy(() -> copilotApiController.chat("skills", agUiParameters))
            .isInstanceOf(AccessDeniedException.class);

        verify(agUiService, never()).runAgent(any(), any());
        verifyNoInteractions(userService);
    }

    @Test
    void testClientSuppliedAuthenticatedUserIdIsReplacedByThePlatformUser() {
        setAuthentication(new UsernamePasswordAuthenticationToken(PLATFORM_LOGIN, "", List.of()));

        User user = new User();

        user.setId(PLATFORM_USER_ID);

        when(userService.fetchUserByLogin(PLATFORM_LOGIN)).thenReturn(Optional.of(user));
        when(agUiService.runAgent(any(), any())).thenReturn(new SseEmitter());

        AgUiParameters agUiParameters = newAgUiParameters();

        newCopilotApiController().chat("skills", agUiParameters);

        Map<String, Object> stateMap = agUiParameters.getState()
            .getState();

        assertThat(stateMap).containsEntry(CopilotConstants.STATE_AUTHENTICATED_USER_ID, PLATFORM_USER_ID);
        verify(agUiService).runAgent(localAgent, agUiParameters);
    }

    @Test
    void testClientSuppliedAuthenticatedUserIdIsDroppedWithoutAPlatformUser() {
        setAuthentication(new UsernamePasswordAuthenticationToken("unknown", "", List.of()));

        when(userService.fetchUserByLogin("unknown")).thenReturn(Optional.empty());
        when(agUiService.runAgent(any(), any())).thenReturn(new SseEmitter());

        AgUiParameters agUiParameters = newAgUiParameters();

        newCopilotApiController().chat("skills", agUiParameters);

        assertThat(agUiParameters.getState()
            .getState()).doesNotContainKey(CopilotConstants.STATE_AUTHENTICATED_USER_ID);
    }

    private CopilotApiController newCopilotApiController() {
        when(localAgent.getAgentId()).thenReturn("skills_ask");

        return new CopilotApiController(
            agUiService, List.of(localAgent), Optional.of(mock(PermissionService.class)),
            Optional.of(mock(ProjectWorkflowService.class)), Optional.of(userService));
    }

    private static AgUiParameters newAgUiParameters() {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put("mode", "ASK");
        stateMap.put(CopilotConstants.STATE_AUTHENTICATED_USER_ID, FORGED_USER_ID);

        AgUiParameters agUiParameters = new AgUiParameters();

        agUiParameters.setState(new State(stateMap));

        return agUiParameters;
    }

    private static void setAuthentication(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }
}
