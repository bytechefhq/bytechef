/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.workspaceprompt.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.workspaceprompt.domain.WorkspaceSystemPrompt;
import com.bytechef.ee.platform.ai.workspaceprompt.service.WorkspaceSystemPromptService;
import com.bytechef.graphql.error.GraphQlBadRequestException;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 */
class WorkspaceSystemPromptGraphQlControllerTest {

    private final WorkspaceSystemPromptService service = mock(WorkspaceSystemPromptService.class);
    private final WorkspaceSystemPromptGraphQlController controller =
        new WorkspaceSystemPromptGraphQlController(service);

    @Test
    void testQueryReturnsNullWhenUnset() {
        when(service.fetchWorkspaceSystemPrompt(7L)).thenReturn(Optional.empty());

        assertThat(controller.workspaceSystemPrompt(7L)).isNull();
    }

    @Test
    void testQueryReturnsStoredPrompt() {
        when(service.fetchWorkspaceSystemPrompt(7L)).thenReturn(Optional.of("Be concise."));

        assertThat(controller.workspaceSystemPrompt(7L))
            .isEqualTo(new WorkspaceSystemPrompt(7L, "Be concise."));
    }

    @Test
    void testUpdateSavesAndEchoes() {
        when(service.saveWorkspaceSystemPrompt(7L, "Be concise.")).thenReturn(Optional.of("Be concise."));

        WorkspaceSystemPrompt result = controller.updateWorkspaceSystemPrompt(
            new WorkspaceSystemPromptGraphQlController.WorkspaceSystemPromptInput(7L, "Be concise."));

        assertThat(result).isEqualTo(new WorkspaceSystemPrompt(7L, "Be concise."));
    }

    @Test
    void testUpdateBlankReturnsNull() {
        when(service.saveWorkspaceSystemPrompt(7L, "   ")).thenReturn(Optional.empty());

        assertThat(controller.updateWorkspaceSystemPrompt(
            new WorkspaceSystemPromptGraphQlController.WorkspaceSystemPromptInput(7L, "   "))).isNull();
    }

    @Test
    void testMutationIsAdminGated() throws NoSuchMethodException {
        Method method = WorkspaceSystemPromptGraphQlController.class.getMethod(
            "updateWorkspaceSystemPrompt", WorkspaceSystemPromptGraphQlController.WorkspaceSystemPromptInput.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("ROLE_ADMIN");
    }

    @Test
    void testQueryIsPermissionGated() throws NoSuchMethodException {
        Method method = WorkspaceSystemPromptGraphQlController.class.getMethod("workspaceSystemPrompt", long.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("ROLE_ADMIN");
        assertThat(preAuthorize.value()).contains("AI_GATEWAY_VIEW");
    }

    @Test
    void testUpdateRejectsOverLengthPrompt() {
        String overLengthPrompt = "a".repeat(WorkspaceSystemPrompt.MAX_LENGTH + 1);

        assertThatThrownBy(() -> controller.updateWorkspaceSystemPrompt(
            new WorkspaceSystemPromptGraphQlController.WorkspaceSystemPromptInput(7L, overLengthPrompt)))
                .isInstanceOf(GraphQlBadRequestException.class);

        verifyNoInteractions(service);
    }
}
