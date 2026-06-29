/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.exception;

import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.exception.AbstractErrorType;

/**
 * Error types surfaced by {@link com.bytechef.ee.automation.configuration.service.CustomRoleServiceImpl} and by the
 * {@code CustomRoleGraphQlController} input validation. Lives in the api module so the GraphQL controller (which only
 * depends on the api, not the service) can throw {@code ConfigurationException} with these error types instead of
 * surfacing validation failures as generic {@code INTERNAL_ERROR} via raw {@code IllegalArgumentException}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CustomRoleErrorType extends AbstractErrorType {

    public static final CustomRoleErrorType CUSTOM_ROLE_IN_USE = new CustomRoleErrorType(100);
    public static final CustomRoleErrorType INVALID_SCOPE = new CustomRoleErrorType(101);
    public static final CustomRoleErrorType SCOPES_REQUIRED = new CustomRoleErrorType(102);

    private CustomRoleErrorType(int errorKey) {
        super(CustomRole.class, errorKey);
    }
}
