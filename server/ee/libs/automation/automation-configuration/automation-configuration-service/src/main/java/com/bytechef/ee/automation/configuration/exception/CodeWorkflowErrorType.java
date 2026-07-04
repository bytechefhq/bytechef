/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.exception;

import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.exception.AbstractErrorType;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CodeWorkflowErrorType extends AbstractErrorType {

    public static final CodeWorkflowErrorType JAVA_CODE_WORKFLOW_UPLOAD_DISABLED = new CodeWorkflowErrorType(100);

    private CodeWorkflowErrorType(int errorKey) {
        super(CodeWorkflowContainer.class, errorKey);
    }
}
