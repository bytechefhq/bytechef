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

    public static final CodeWorkflowErrorType LANGUAGE_NOT_SUPPORTED = new CodeWorkflowErrorType(101);

    public static final CodeWorkflowErrorType INVALID_CODE_WORKFLOW_NAME = new CodeWorkflowErrorType(102);

    public static final CodeWorkflowErrorType CODE_WORKFLOW_NAME_MISMATCH = new CodeWorkflowErrorType(103);

    public static final CodeWorkflowErrorType SOURCE_LOAD_FAILED = new CodeWorkflowErrorType(104);

    public static final CodeWorkflowErrorType CODE_WORKFLOW_ALREADY_EXISTS = new CodeWorkflowErrorType(105);

    public static final CodeWorkflowErrorType EMBEDDED_BRIDGE_PROJECT_NOT_EDITABLE = new CodeWorkflowErrorType(106);

    private CodeWorkflowErrorType(int errorKey) {
        super(CodeWorkflowContainer.class, errorKey);
    }
}
