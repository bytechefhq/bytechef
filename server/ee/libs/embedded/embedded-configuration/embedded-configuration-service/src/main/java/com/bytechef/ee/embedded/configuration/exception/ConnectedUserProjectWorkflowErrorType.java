/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.exception;

import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.exception.AbstractErrorType;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ConnectedUserProjectWorkflowErrorType extends AbstractErrorType {

    public static final ConnectedUserProjectWorkflowErrorType WORKFLOW_TEMPLATE_ALREADY_COPIED =
        new ConnectedUserProjectWorkflowErrorType(100);

    private ConnectedUserProjectWorkflowErrorType(int errorKey) {
        super(ConnectedUserProjectWorkflow.class, errorKey);
    }
}
