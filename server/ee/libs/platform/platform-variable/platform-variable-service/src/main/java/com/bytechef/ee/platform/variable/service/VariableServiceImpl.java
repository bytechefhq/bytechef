/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.variable.service;

import com.bytechef.ee.platform.variable.domain.Variable;
import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.ee.platform.variable.exception.VariableErrorType;
import com.bytechef.ee.platform.variable.validator.VariableNameValidator;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One {@link Property} row per variable: key {@code variable.<name>}, value map {@code {"value": <string>}},
 * {@code Scope.WORKSPACE}/workspaceId or {@code Scope.EMBEDDED}/null, environment always set. Ids are property row ids;
 * every by-id operation re-lists the scope so an id from another scope is indistinguishable from a missing one.
 *
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class VariableServiceImpl implements VariableService {

    private final PropertyService propertyService;

    @SuppressFBWarnings("EI")
    public VariableServiceImpl(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @Override
    public Variable create(VariableScope scope, long environmentId, String name, String value) {
        VariableNameValidator.validate(name, value);

        String key = Variable.KEY_PREFIX + name;
        Property.Scope propertyScope = toPropertyScope(scope);

        if (propertyService.fetchProperty(key, propertyScope, scope.workspaceId(), environmentId)
            .isPresent()) {

            throw new ConfigurationException(
                "Variable '" + name + "' already exists", VariableErrorType.VARIABLE_NAME_ALREADY_EXISTS);
        }

        saveProperty(key, value, propertyScope, scope, environmentId, name);

        return toVariable(fetchSavedProperty(key, propertyScope, scope.workspaceId(), environmentId));
    }

    @Override
    public void delete(VariableScope scope, long environmentId, long id) {
        Property property = getProperty(scope, environmentId, id);

        propertyService.delete(property.getKey(), toPropertyScope(scope), scope.workspaceId(), environmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getVariableMap(VariableScope scope, long environmentId) {
        Map<String, String> variableMap = new LinkedHashMap<>();

        for (Variable variable : getVariables(scope, environmentId)) {
            variableMap.put(variable.name(), variable.value());
        }

        return variableMap;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Variable> getVariables(VariableScope scope, long environmentId) {
        List<Property> properties = propertyService.getPropertiesByKeyPrefix(
            Variable.KEY_PREFIX, toPropertyScope(scope), scope.workspaceId(), environmentId);

        return properties.stream()
            .map(VariableServiceImpl::toVariable)
            .sorted((firstVariable, secondVariable) -> firstVariable.name()
                .compareToIgnoreCase(secondVariable.name()))
            .toList();
    }

    @Override
    public Variable update(VariableScope scope, long environmentId, long id, String name, String value) {
        VariableNameValidator.validate(name, value);

        Property existing = getProperty(scope, environmentId, id);
        Property.Scope propertyScope = toPropertyScope(scope);
        String newKey = Variable.KEY_PREFIX + name;

        if (!Objects.equals(existing.getKey(), newKey)) {
            if (propertyService.fetchProperty(newKey, propertyScope, scope.workspaceId(), environmentId)
                .isPresent()) {

                throw new ConfigurationException(
                    "Variable '" + name + "' already exists", VariableErrorType.VARIABLE_NAME_ALREADY_EXISTS);
            }

            propertyService.delete(existing.getKey(), propertyScope, scope.workspaceId(), environmentId);
        }

        saveProperty(newKey, value, propertyScope, scope, environmentId, name);

        return toVariable(fetchSavedProperty(newKey, propertyScope, scope.workspaceId(), environmentId));
    }

    /**
     * Saves the property row, translating a unique-index violation into the same
     * {@link VariableErrorType#VARIABLE_NAME_ALREADY_EXISTS} the pre-save {@code fetchProperty} check above throws. The
     * pre-save check only rejects a duplicate that already committed before this call started; a second concurrent
     * create/rename racing for the same name can still lose to {@code uk_property_key_scope_scope_id_environment}
     * (workspace scope) or {@code uk_property_key_scope_environment_null_scope_id} (embedded/platform scope, null
     * {@code scope_id}) at the database, surfacing as {@link DuplicateKeyException} -- caught here rather than left to
     * bubble up as an opaque 500. Deliberately narrower than the umbrella
     * {@link org.springframework.dao.DataIntegrityViolationException}: a unique-index hit is the only integrity
     * violation this method should ever reinterpret as "already exists" -- a future NOT NULL or FK violation on this
     * shared table must still surface as a genuine failure rather than being mislabeled as a duplicate name.
     */
    private void saveProperty(
        String key, String value, Property.Scope propertyScope, VariableScope scope, long environmentId,
        String name) {

        try {
            propertyService.save(key, Map.of(Variable.VALUE_KEY, value), propertyScope, scope.workspaceId(),
                environmentId);
        } catch (DuplicateKeyException duplicateKeyException) {
            throw new ConfigurationException(
                "Variable '" + name + "' already exists", duplicateKeyException,
                VariableErrorType.VARIABLE_NAME_ALREADY_EXISTS);
        }
    }

    /**
     * Re-lists the scope and locates {@code id} within it, so an id belonging to another scope's variable is
     * indistinguishable from a missing one -- ids must never be usable to reach another scope's rows.
     */
    private Property getProperty(VariableScope scope, long environmentId, long id) {
        List<Property> properties = propertyService.getPropertiesByKeyPrefix(
            Variable.KEY_PREFIX, toPropertyScope(scope), scope.workspaceId(), environmentId);

        return properties.stream()
            .filter(property -> Objects.equals(property.getId(), id))
            .findFirst()
            .orElseThrow(() -> new ConfigurationException(
                "Variable not found: " + id, VariableErrorType.VARIABLE_NOT_FOUND));
    }

    /**
     * Re-reads the row just written by {@link PropertyService#save}, so the returned {@link Variable} carries the
     * generated id and audit fields rather than ones assembled by hand.
     */
    private Property fetchSavedProperty(
        String key, Property.Scope propertyScope, @Nullable Long workspaceId, long environmentId) {

        return propertyService.fetchProperty(key, propertyScope, workspaceId, environmentId)
            .orElseThrow(
                () -> new IllegalStateException("Variable '" + key + "' was not found right after being saved"));
    }

    private static Property.Scope toPropertyScope(VariableScope scope) {
        return switch (scope.type()) {
            case WORKSPACE -> Property.Scope.WORKSPACE;
            case EMBEDDED -> Property.Scope.EMBEDDED;
        };
    }

    private static Variable toVariable(Property property) {
        String key = property.getKey();
        Object value = property.get(Variable.VALUE_KEY);

        // Every variable row is written with an environment (see the class javadoc), so a null here means the row
        // was not written by this service -- fail loudly instead of defaulting to a real environment id (0 is
        // DEVELOPMENT) and silently mislabeling it.
        int environment = Objects.requireNonNull(property.getEnvironment(), "environment");

        return new Variable(
            Objects.requireNonNull(property.getId(), "id"), key.substring(Variable.KEY_PREFIX.length()),
            value == null ? "" : String.valueOf(value), environment, property.getCreatedBy(),
            property.getCreatedDate(), property.getLastModifiedBy(), property.getLastModifiedDate());
    }
}
