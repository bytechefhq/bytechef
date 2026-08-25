/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.variable.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.variable.domain.Variable;
import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.ee.platform.variable.exception.VariableErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

/**
 * @version ee
 */
class VariableServiceTest {

    private PropertyService propertyService;
    private VariableServiceImpl variableService;

    @BeforeEach
    void beforeEach() {
        propertyService = mock(PropertyService.class);
        variableService = new VariableServiceImpl(propertyService);
    }

    @Test
    void testGetVariablesMapsWorkspaceScopeRows() {
        Property property = property(11L, "variable.API_URL", "https://api", Property.Scope.WORKSPACE, 7L, 1);

        when(propertyService.getPropertiesByKeyPrefix("variable.", Property.Scope.WORKSPACE, 7L, 1L))
            .thenReturn(List.of(property));

        List<Variable> variables = variableService.getVariables(VariableScope.workspace(7L), 1L);

        assertThat(variables).hasSize(1);
        assertThat(variables.getFirst()
            .name()).isEqualTo("API_URL");
        assertThat(variables.getFirst()
            .value()).isEqualTo("https://api");
        assertThat(variables.getFirst()
            .id()).isEqualTo(11L);
    }

    @Test
    void testGetVariableMapUsesEmbeddedScopeWithNullScopeId() {
        when(propertyService.getPropertiesByKeyPrefix("variable.", Property.Scope.EMBEDDED, null, 2L))
            .thenReturn(List.of(property(1L, "variable.REGION", "eu", Property.Scope.EMBEDDED, null, 2)));

        Map<String, String> map = variableService.getVariableMap(VariableScope.embedded(), 2L);

        assertThat(map).containsExactly(Map.entry("REGION", "eu"));
    }

    @Test
    void testCreateSavesPrefixedKeyAndRejectsDuplicates() {
        when(propertyService.fetchProperty("variable.API_URL", Property.Scope.WORKSPACE, 7L, 1L))
            .thenReturn(Optional.empty(), Optional.of(
                property(5L, "variable.API_URL", "https://api", Property.Scope.WORKSPACE, 7L, 1)));

        Variable created = variableService.create(VariableScope.workspace(7L), 1L, "API_URL", "https://api");

        verify(propertyService).save(
            "variable.API_URL", Map.of("value", "https://api"), Property.Scope.WORKSPACE, 7L, 1L);
        assertThat(created.id()).isEqualTo(5L);

        assertThatThrownBy(() -> variableService.create(VariableScope.workspace(7L), 1L, "API_URL", "x"))
            .asInstanceOf(type(ConfigurationException.class))
            .extracting(ConfigurationException::getErrorKey)
            .isEqualTo(VariableErrorType.VARIABLE_NAME_ALREADY_EXISTS.getErrorKey());
    }

    @Test
    void testCreateTranslatesConstraintViolationForWorkspaceScope() {
        // Simulates the losing side of a race: the pre-save fetchProperty check misses the concurrent winner
        // (it has not committed yet), so save() is the one that hits uk_property_key_scope_scope_id_environment.
        when(propertyService.fetchProperty("variable.RACE", Property.Scope.WORKSPACE, 7L, 1L))
            .thenReturn(Optional.empty());
        doThrow(new DuplicateKeyException("duplicate key"))
            .when(propertyService)
            .save(eq("variable.RACE"), any(), eq(Property.Scope.WORKSPACE), eq(7L), eq(1L));

        assertThatThrownBy(() -> variableService.create(VariableScope.workspace(7L), 1L, "RACE", "x"))
            .asInstanceOf(type(ConfigurationException.class))
            .extracting(ConfigurationException::getErrorKey)
            .isEqualTo(VariableErrorType.VARIABLE_NAME_ALREADY_EXISTS.getErrorKey());
    }

    @Test
    void testCreateTranslatesConstraintViolationForEmbeddedScope() {
        // Same race, but for the null-scope_id shape: the constraint that catches it is the partial unique index
        // uk_property_key_scope_environment_null_scope_id, not uk_property_key_scope_scope_id_environment.
        when(propertyService.fetchProperty("variable.RACE", Property.Scope.EMBEDDED, null, 2L))
            .thenReturn(Optional.empty());
        doThrow(new DuplicateKeyException("duplicate key"))
            .when(propertyService)
            .save(eq("variable.RACE"), any(), eq(Property.Scope.EMBEDDED), isNull(), eq(2L));

        assertThatThrownBy(() -> variableService.create(VariableScope.embedded(), 2L, "RACE", "x"))
            .asInstanceOf(type(ConfigurationException.class))
            .extracting(ConfigurationException::getErrorKey)
            .isEqualTo(VariableErrorType.VARIABLE_NAME_ALREADY_EXISTS.getErrorKey());
    }

    @Test
    void testUpdateTranslatesConstraintViolationOnRename() {
        // The rename path has its own pre-save duplicate check (see testUpdateRenamesByDeletingOldKeyAndSavingNew),
        // but the same race can still slip past it and hit the constraint on the insert of the renamed row.
        when(propertyService.getPropertiesByKeyPrefix("variable.", Property.Scope.WORKSPACE, 7L, 1L))
            .thenReturn(List.of(property(5L, "variable.OLD", "a", Property.Scope.WORKSPACE, 7L, 1)));
        when(propertyService.fetchProperty("variable.NEW", Property.Scope.WORKSPACE, 7L, 1L))
            .thenReturn(Optional.empty());
        doThrow(new DuplicateKeyException("duplicate key"))
            .when(propertyService)
            .save(eq("variable.NEW"), any(), eq(Property.Scope.WORKSPACE), eq(7L), eq(1L));

        assertThatThrownBy(() -> variableService.update(VariableScope.workspace(7L), 1L, 5L, "NEW", "b"))
            .asInstanceOf(type(ConfigurationException.class))
            .extracting(ConfigurationException::getErrorKey)
            .isEqualTo(VariableErrorType.VARIABLE_NAME_ALREADY_EXISTS.getErrorKey());
    }

    @Test
    void testCreateRejectsInvalidName() {
        assertThatThrownBy(() -> variableService.create(VariableScope.workspace(7L), 1L, "1bad", "x"))
            .isInstanceOf(ConfigurationException.class);

        verify(propertyService, never()).save(eq("variable.1bad"), eq(Map.of("value", "x")),
            eq(Property.Scope.WORKSPACE),
            eq(7L), eq(1L));
    }

    @Test
    void testUpdateRenamesByDeletingOldKeyAndSavingNew() {
        Property existing = property(5L, "variable.OLD", "a", Property.Scope.WORKSPACE, 7L, 1);

        when(propertyService.getPropertiesByKeyPrefix("variable.", Property.Scope.WORKSPACE, 7L, 1L))
            .thenReturn(List.of(existing));
        when(propertyService.fetchProperty("variable.NEW", Property.Scope.WORKSPACE, 7L, 1L))
            .thenReturn(
                Optional.empty(), Optional.of(property(9L, "variable.NEW", "b", Property.Scope.WORKSPACE, 7L, 1)));

        Variable updated = variableService.update(VariableScope.workspace(7L), 1L, 5L, "NEW", "b");

        verify(propertyService).delete("variable.OLD", Property.Scope.WORKSPACE, 7L, 1L);
        verify(propertyService).save("variable.NEW", Map.of("value", "b"), Property.Scope.WORKSPACE, 7L, 1L);
        assertThat(updated.name()).isEqualTo("NEW");
    }

    @Test
    void testUpdateWithSameNameOnlySaves() {
        when(propertyService.getPropertiesByKeyPrefix("variable.", Property.Scope.WORKSPACE, 7L, 1L))
            .thenReturn(List.of(property(5L, "variable.SAME", "a", Property.Scope.WORKSPACE, 7L, 1)));
        when(propertyService.fetchProperty("variable.SAME", Property.Scope.WORKSPACE, 7L, 1L))
            .thenReturn(Optional.of(property(5L, "variable.SAME", "b", Property.Scope.WORKSPACE, 7L, 1)));

        variableService.update(VariableScope.workspace(7L), 1L, 5L, "SAME", "b");

        verify(propertyService, never()).delete(eq("variable.SAME"), eq(Property.Scope.WORKSPACE), eq(7L), eq(1L));
        verify(propertyService).save("variable.SAME", Map.of("value", "b"), Property.Scope.WORKSPACE, 7L, 1L);
    }

    @Test
    void testUpdateAndDeleteRejectIdOutsideScope() {
        when(propertyService.getPropertiesByKeyPrefix("variable.", Property.Scope.WORKSPACE, 7L, 1L))
            .thenReturn(List.of());

        assertThatThrownBy(() -> variableService.update(VariableScope.workspace(7L), 1L, 99L, "X", "y"))
            .asInstanceOf(type(ConfigurationException.class))
            .extracting(ConfigurationException::getErrorKey)
            .isEqualTo(VariableErrorType.VARIABLE_NOT_FOUND.getErrorKey());
        assertThatThrownBy(() -> variableService.delete(VariableScope.workspace(7L), 1L, 99L))
            .asInstanceOf(type(ConfigurationException.class))
            .extracting(ConfigurationException::getErrorKey)
            .isEqualTo(VariableErrorType.VARIABLE_NOT_FOUND.getErrorKey());
    }

    @Test
    void testDeleteRemovesRow() {
        when(propertyService.getPropertiesByKeyPrefix("variable.", Property.Scope.WORKSPACE, 7L, 1L))
            .thenReturn(List.of(property(5L, "variable.GONE", "a", Property.Scope.WORKSPACE, 7L, 1)));

        variableService.delete(VariableScope.workspace(7L), 1L, 5L);

        verify(propertyService).delete("variable.GONE", Property.Scope.WORKSPACE, 7L, 1L);
    }

    private static Property property(
        long id, String key, String value, Property.Scope scope, Long scopeId, int environment) {

        Property property = new Property();

        property.setId(id);
        property.setKey(key);
        property.setScope(scope);
        property.setScopeId(scopeId);
        property.setEnvironment(environment);
        property.setEnabled(true);
        property.setValue(Map.of("value", value));

        return property;
    }
}
