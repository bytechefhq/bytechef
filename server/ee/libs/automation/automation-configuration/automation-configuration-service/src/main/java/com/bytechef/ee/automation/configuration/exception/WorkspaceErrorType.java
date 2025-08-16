/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.exception;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.exception.AbstractErrorType;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class WorkspaceErrorType extends AbstractErrorType {

    public static final WorkspaceErrorType DEFAULT_WORKSPACE_NOT_DELETABLE = new WorkspaceErrorType(100);
    public static final WorkspaceErrorType DEFAULT_WORKSPACE_NOT_CHANGEABLE = new WorkspaceErrorType(101);

    private WorkspaceErrorType(int errorKey) {
        super(Workspace.class, errorKey);
    }
}
