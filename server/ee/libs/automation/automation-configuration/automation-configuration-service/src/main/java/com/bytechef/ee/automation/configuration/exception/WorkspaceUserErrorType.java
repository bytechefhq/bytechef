/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.exception;

import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.exception.AbstractErrorType;

/**
 * Error types surfaced by {@link com.bytechef.ee.automation.configuration.service.WorkspaceUserServiceImpl}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class WorkspaceUserErrorType extends AbstractErrorType {

    public static final WorkspaceUserErrorType ALREADY_MEMBER = new WorkspaceUserErrorType(100);
    public static final WorkspaceUserErrorType NOT_MEMBER = new WorkspaceUserErrorType(101);
    public static final WorkspaceUserErrorType LAST_ADMIN_PROTECTED = new WorkspaceUserErrorType(102);
    public static final WorkspaceUserErrorType SELF_DEMOTION_FORBIDDEN = new WorkspaceUserErrorType(103);

    private WorkspaceUserErrorType(int errorKey) {
        super(WorkspaceUser.class, errorKey);
    }
}
