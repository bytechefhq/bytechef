/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.facade;

import com.bytechef.ee.platform.user.scim.exception.ScimBadRequestException;
import com.bytechef.ee.platform.user.scim.model.ScimPatchOperation;
import com.bytechef.ee.platform.user.scim.model.ScimPatchRequest;
import com.bytechef.ee.platform.user.scim.model.ScimUser;
import com.bytechef.platform.user.constant.UserConstants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class ScimUserPatchOperations {

    private static final String ACTIVE = "active";
    private static final String ADD = "add";
    private static final String EXTERNAL_ID = "externalId";
    private static final String FAMILY_NAME = "name.familyName";
    private static final String GIVEN_NAME = "name.givenName";
    private static final String NAME = "name";
    private static final String REMOVE = "remove";
    private static final String REPLACE = "replace";
    private static final String USER_NAME = "userName";
    private static final String USER_SCHEMA_PREFIX = ScimUser.SCHEMA.toLowerCase(Locale.ROOT) + ":";

    private ScimUserPatchOperations() {
    }

    static List<AttributeChange> parse(ScimPatchRequest scimPatchRequest) {
        List<ScimPatchOperation> scimPatchOperations = scimPatchRequest.operations();

        if (scimPatchOperations == null || scimPatchOperations.isEmpty()) {
            throw ScimBadRequestException.invalidSyntax("A PATCH request requires at least one operation");
        }

        List<AttributeChange> attributeChanges = new ArrayList<>();

        for (ScimPatchOperation scimPatchOperation : scimPatchOperations) {
            if (scimPatchOperation == null || scimPatchOperation.operation() == null) {
                throw ScimBadRequestException.invalidSyntax("Every PATCH operation requires an \"op\"");
            }

            String operation = scimPatchOperation.operation();

            switch (operation.toLowerCase(Locale.ROOT)) {
                case ADD, REPLACE -> parseAddOrReplace(scimPatchOperation, attributeChanges);
                case REMOVE -> parseRemove(scimPatchOperation, attributeChanges);
                default -> throw ScimBadRequestException.invalidSyntax("Unsupported PATCH operation: " + operation);
            }
        }

        return attributeChanges;
    }

    private static void parseAddOrReplace(
        ScimPatchOperation scimPatchOperation, List<AttributeChange> attributeChanges) {

        String path = scimPatchOperation.path();
        Object value = scimPatchOperation.value();

        if (path != null && !path.isBlank()) {
            addValue(path, value, attributeChanges);

            return;
        }

        if (!(value instanceof Map<?, ?> valueMap)) {
            throw ScimBadRequestException.invalidSyntax(
                "A PATCH operation without a \"path\" requires an object \"value\"");
        }

        for (Map.Entry<?, ?> entry : valueMap.entrySet()) {
            addValue(String.valueOf(entry.getKey()), entry.getValue(), attributeChanges);
        }
    }

    private static void addValue(String path, @Nullable Object value, List<AttributeChange> attributeChanges) {
        switch (normalizePath(path)) {
            case "active" -> attributeChanges.add(new AttributeChange(Attribute.ACTIVE, toActive(value)));
            case "externalid" -> attributeChanges.add(
                new AttributeChange(
                    Attribute.EXTERNAL_ID,
                    ScimValues.checkMaxLength(
                        EXTERNAL_ID, toSingleValue(EXTERNAL_ID, value), ScimValues.EXTERNAL_ID_MAX_LENGTH)));
            case "username" -> attributeChanges.add(new AttributeChange(Attribute.USER_NAME, toLogin(value)));
            case "name.givenname" -> attributeChanges.add(
                new AttributeChange(
                    Attribute.GIVEN_NAME,
                    ScimValues.checkMaxLength(
                        GIVEN_NAME, toSingleValue(GIVEN_NAME, value), ScimValues.NAME_MAX_LENGTH)));
            case "name.familyname" -> attributeChanges.add(
                new AttributeChange(
                    Attribute.FAMILY_NAME,
                    ScimValues.checkMaxLength(
                        FAMILY_NAME, toSingleValue(FAMILY_NAME, value), ScimValues.NAME_MAX_LENGTH)));
            case "name" -> addNameValue(value, attributeChanges);
            default -> {
            }
        }
    }

    private static void addNameValue(@Nullable Object value, List<AttributeChange> attributeChanges) {
        if (value == null) {
            attributeChanges.add(new AttributeChange(Attribute.GIVEN_NAME, null));
            attributeChanges.add(new AttributeChange(Attribute.FAMILY_NAME, null));

            return;
        }

        if (!(value instanceof Map<?, ?> nameMap)) {
            throw ScimBadRequestException.invalidValue(NAME + " must be an object");
        }

        for (Map.Entry<?, ?> entry : nameMap.entrySet()) {
            addValue(NAME + "." + entry.getKey(), entry.getValue(), attributeChanges);
        }
    }

    private static void parseRemove(ScimPatchOperation scimPatchOperation, List<AttributeChange> attributeChanges) {
        String path = scimPatchOperation.path();

        if (path == null || path.isBlank()) {
            throw ScimBadRequestException.noTarget("A remove operation requires a \"path\"");
        }

        switch (normalizePath(path)) {
            case "active" -> throw ScimBadRequestException.mutability(ACTIVE + " cannot be removed");
            case "username" -> throw ScimBadRequestException.mutability(
                USER_NAME + " is required and cannot be removed");
            case "externalid" -> attributeChanges.add(new AttributeChange(Attribute.EXTERNAL_ID, null));
            case "name.givenname" -> attributeChanges.add(new AttributeChange(Attribute.GIVEN_NAME, null));
            case "name.familyname" -> attributeChanges.add(new AttributeChange(Attribute.FAMILY_NAME, null));
            case "name" -> addNameValue(null, attributeChanges);
            default -> {
            }
        }
    }

    private static String normalizePath(String path) {
        String normalizedPath = path.strip()
            .toLowerCase(Locale.ROOT);

        if (normalizedPath.startsWith(USER_SCHEMA_PREFIX)) {
            return normalizedPath.substring(USER_SCHEMA_PREFIX.length());
        }

        return normalizedPath;
    }

    private static String toActive(@Nullable Object value) {
        if (value instanceof Boolean active) {
            return active.toString();
        }

        if (value instanceof String active && ("true".equalsIgnoreCase(active) || "false".equalsIgnoreCase(active))) {
            return active.toLowerCase(Locale.ROOT);
        }

        throw ScimBadRequestException.invalidValue(ACTIVE + " must be a boolean");
    }

    private static String toLogin(@Nullable Object value) {
        String userName = ScimValues.checkRequired(USER_NAME, toSingleValue(USER_NAME, value));

        String login = userName.toLowerCase(Locale.ROOT);

        ScimValues.checkMaxLength(USER_NAME, login, UserConstants.LOGIN_MAX_LENGTH);

        return login;
    }

    private static @Nullable String toSingleValue(String attributeName, @Nullable Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Map<?, ?> || value instanceof Collection<?>) {
            throw ScimBadRequestException.invalidValue(attributeName + " must be a single value");
        }

        return String.valueOf(value);
    }

    enum Attribute {
        ACTIVE, EXTERNAL_ID, FAMILY_NAME, GIVEN_NAME, USER_NAME
    }

    record AttributeChange(Attribute attribute, @Nullable String value) {
    }
}
