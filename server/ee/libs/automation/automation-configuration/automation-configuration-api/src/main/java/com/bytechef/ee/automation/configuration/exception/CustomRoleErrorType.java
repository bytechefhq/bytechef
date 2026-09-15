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

    /**
     * No custom role with that id exists. Raised by the delete and the update, which used to succeed silently and throw
     * a bare {@code NoSuchElementException} respectively — the first publishing a {@code CUSTOM_ROLE_DELETED} audit
     * event for a role nobody deleted, the second surfacing as {@code INTERNAL_ERROR}.
     */
    public static final CustomRoleErrorType CUSTOM_ROLE_NOT_FOUND = new CustomRoleErrorType(103);

    /**
     * Another custom role already holds that name, which {@code uk_custom_role_name} forbids. Checked before the write
     * rather than caught afterwards: PostgreSQL aborts the whole transaction on a constraint violation, so a caught
     * {@code DuplicateKeyException} would turn a 500 into a handled error that still fails at commit.
     */
    public static final CustomRoleErrorType DUPLICATE_NAME = new CustomRoleErrorType(104);

    /**
     * The name is one of the built-in roles, such as {@code ADMIN}, compared ignoring case. A custom role with that
     * name would read as the built-in role wherever roles are listed by name.
     */
    public static final CustomRoleErrorType RESERVED_NAME = new CustomRoleErrorType(105);

    private CustomRoleErrorType(int errorKey) {
        super(CustomRole.class, errorKey);
    }
}
