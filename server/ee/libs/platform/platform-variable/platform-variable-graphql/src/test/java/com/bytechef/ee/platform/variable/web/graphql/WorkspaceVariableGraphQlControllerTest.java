/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.variable.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.variable.domain.Variable;
import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.ee.platform.variable.service.VariableService;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @version ee
 */
class WorkspaceVariableGraphQlControllerTest {

    private VariableService variableService;
    private WorkspaceVariableGraphQlController controller;

    @BeforeEach
    void beforeEach() {
        variableService = mock(VariableService.class);
        controller = new WorkspaceVariableGraphQlController(variableService);
    }

    @Test
    void testWorkspaceVariablesRequiresVariableView() throws NoSuchMethodException {
        Method method = WorkspaceVariableGraphQlController.class.getDeclaredMethod(
            "workspaceVariables", long.class, long.class);

        assertThat(method.getAnnotation(QueryMapping.class)).isNotNull();
        assertThat(method.getAnnotation(PreAuthorize.class)
            .value()).isEqualTo("hasPermission(#workspaceId, 'Workspace', 'VARIABLE_VIEW')");
    }

    @Test
    void testMutationsRequireVariableManage() throws NoSuchMethodException {
        for (Method method : new Method[] {
            WorkspaceVariableGraphQlController.class.getDeclaredMethod(
                "createWorkspaceVariable", long.class, long.class,
                WorkspaceVariableGraphQlController.VariableInput.class),
            WorkspaceVariableGraphQlController.class.getDeclaredMethod(
                "updateWorkspaceVariable", long.class, long.class, long.class,
                WorkspaceVariableGraphQlController.VariableInput.class),
            WorkspaceVariableGraphQlController.class.getDeclaredMethod(
                "deleteWorkspaceVariable", long.class, long.class, long.class)
        }) {
            assertThat(method.getAnnotation(MutationMapping.class)).isNotNull();
            assertThat(method.getAnnotation(PreAuthorize.class)
                .value()).isEqualTo("hasPermission(#workspaceId, 'Workspace', 'VARIABLE_MANAGE')");
        }
    }

    @Test
    void testWorkspaceVariablesDelegatesWithWorkspaceScope() {
        Variable variable = new Variable(1L, "A", "1", 0, null, null, null, null);

        when(variableService.getVariables(VariableScope.workspace(7L), 0L)).thenReturn(List.of(variable));

        assertThat(controller.workspaceVariables(7L, 0L)).containsExactly(
            new WorkspaceVariableGraphQlController.VariableResponse(1L, "A", "1", 0, null, null, null, null));
    }

    @Test
    void testWorkspaceVariablesMapsInstantFieldsToStrings() {
        Instant createdDate = Instant.parse("2026-01-01T00:00:00Z");
        Instant lastModifiedDate = Instant.parse("2026-01-02T00:00:00Z");
        Variable variable = new Variable(1L, "A", "1", 0, "alice", createdDate, "bob", lastModifiedDate);

        when(variableService.getVariables(VariableScope.workspace(7L), 0L)).thenReturn(List.of(variable));

        assertThat(controller.workspaceVariables(7L, 0L)).containsExactly(
            new WorkspaceVariableGraphQlController.VariableResponse(
                1L, "A", "1", 0, "alice", createdDate.toString(), "bob", lastModifiedDate.toString()));
    }

    @Test
    void testCreateWorkspaceVariableDelegates() {
        Variable variable = new Variable(1L, "A", "1", 0, null, null, null, null);

        when(variableService.create(VariableScope.workspace(7L), 0L, "A", "1")).thenReturn(variable);

        assertThat(controller.createWorkspaceVariable(
            7L, 0L, new WorkspaceVariableGraphQlController.VariableInput("A", "1"))).isEqualTo(
                new WorkspaceVariableGraphQlController.VariableResponse(1L, "A", "1", 0, null, null, null, null));
    }

    @Test
    void testDeleteWorkspaceVariableDelegatesAndReturnsTrue() {
        assertThat(controller.deleteWorkspaceVariable(7L, 0L, 5L)).isTrue();

        verify(variableService).delete(VariableScope.workspace(7L), 0L, 5L);
    }
}
