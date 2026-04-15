/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.exception;

import com.bytechef.ee.automation.configuration.domain.ProjectUser;
import com.bytechef.exception.AbstractErrorType;

/**
 * Error types surfaced by {@link com.bytechef.ee.automation.configuration.service.ProjectUserServiceImpl}. Declaring
 * them as {@link AbstractErrorType} lets the GraphQL resolver map them to user-visible error codes instead of
 * swallowing the underlying {@link IllegalArgumentException}/{@link IllegalStateException} as a generic
 * {@code INTERNAL_ERROR}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ProjectUserErrorType extends AbstractErrorType {

    public static final ProjectUserErrorType ALREADY_MEMBER = new ProjectUserErrorType(100);
    public static final ProjectUserErrorType NOT_MEMBER = new ProjectUserErrorType(101);
    public static final ProjectUserErrorType NOT_WORKSPACE_MEMBER = new ProjectUserErrorType(102);
    public static final ProjectUserErrorType LAST_ADMIN_PROTECTED = new ProjectUserErrorType(103);
    public static final ProjectUserErrorType ROLE_ELEVATION_FORBIDDEN = new ProjectUserErrorType(104);
    public static final ProjectUserErrorType INVALID_ROLE = new ProjectUserErrorType(105);
    public static final ProjectUserErrorType PROJECT_NOT_FOUND = new ProjectUserErrorType(106);
    public static final ProjectUserErrorType SELF_DEMOTION_FORBIDDEN = new ProjectUserErrorType(107);

    private ProjectUserErrorType(int errorKey) {
        super(ProjectUser.class, errorKey);
    }
}
