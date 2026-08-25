/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

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
class EmbeddedVariableGraphQlControllerTest {

    private VariableService variableService;
    private EmbeddedVariableGraphQlController controller;

    @BeforeEach
    void beforeEach() {
        variableService = mock(VariableService.class);
        controller = new EmbeddedVariableGraphQlController(variableService);
    }

    @Test
    void testEmbeddedVariablesRequiresAuthentication() throws NoSuchMethodException {
        Method method = EmbeddedVariableGraphQlController.class.getDeclaredMethod("embeddedVariables", long.class);

        assertThat(method.getAnnotation(QueryMapping.class)).isNotNull();
        assertThat(method.getAnnotation(PreAuthorize.class)
            .value()).isEqualTo("isAuthenticated()");
    }

    @Test
    void testMutationsRequireAdmin() throws NoSuchMethodException {
        for (Method method : new Method[] {
            EmbeddedVariableGraphQlController.class.getDeclaredMethod(
                "createEmbeddedVariable", long.class, EmbeddedVariableGraphQlController.VariableInput.class),
            EmbeddedVariableGraphQlController.class.getDeclaredMethod(
                "updateEmbeddedVariable", long.class, long.class,
                EmbeddedVariableGraphQlController.VariableInput.class),
            EmbeddedVariableGraphQlController.class.getDeclaredMethod(
                "deleteEmbeddedVariable", long.class, long.class)
        }) {
            assertThat(method.getAnnotation(MutationMapping.class)).isNotNull();
            assertThat(method.getAnnotation(PreAuthorize.class)
                .value()).isEqualTo("hasAuthority('ROLE_ADMIN')");
        }
    }

    @Test
    void testEmbeddedVariablesDelegatesWithEmbeddedScope() {
        Variable variable = new Variable(1L, "A", "1", 0, null, null, null, null);

        when(variableService.getVariables(VariableScope.embedded(), 0L)).thenReturn(List.of(variable));

        assertThat(controller.embeddedVariables(0L)).containsExactly(
            new EmbeddedVariableGraphQlController.VariableResponse(1L, "A", "1", 0, null, null, null, null));
    }

    @Test
    void testEmbeddedVariablesMapsInstantFieldsToStrings() {
        Instant createdDate = Instant.parse("2026-01-01T00:00:00Z");
        Instant lastModifiedDate = Instant.parse("2026-01-02T00:00:00Z");
        Variable variable = new Variable(1L, "A", "1", 0, "alice", createdDate, "bob", lastModifiedDate);

        when(variableService.getVariables(VariableScope.embedded(), 0L)).thenReturn(List.of(variable));

        assertThat(controller.embeddedVariables(0L)).containsExactly(
            new EmbeddedVariableGraphQlController.VariableResponse(
                1L, "A", "1", 0, "alice", createdDate.toString(), "bob", lastModifiedDate.toString()));
    }

    @Test
    void testCreateEmbeddedVariableDelegates() {
        Variable variable = new Variable(1L, "A", "1", 0, null, null, null, null);

        when(variableService.create(VariableScope.embedded(), 0L, "A", "1")).thenReturn(variable);

        assertThat(controller.createEmbeddedVariable(
            0L, new EmbeddedVariableGraphQlController.VariableInput("A", "1"))).isEqualTo(
                new EmbeddedVariableGraphQlController.VariableResponse(1L, "A", "1", 0, null, null, null, null));
    }

    @Test
    void testDeleteEmbeddedVariableDelegatesAndReturnsTrue() {
        assertThat(controller.deleteEmbeddedVariable(0L, 5L)).isTrue();

        verify(variableService).delete(VariableScope.embedded(), 0L, 5L);
    }
}
