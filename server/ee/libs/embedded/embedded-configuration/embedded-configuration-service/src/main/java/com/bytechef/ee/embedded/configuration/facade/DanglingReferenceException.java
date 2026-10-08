/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.exception.WorkflowErrorType;
import com.bytechef.exception.ConfigurationException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class DanglingReferenceException extends ConfigurationException {

    DanglingReferenceException(String automationWorkflowUuid) {
        super(
            "Reference to automation workflow %s is dangling".formatted(automationWorkflowUuid),
            WorkflowErrorType.WORKFLOW_NOT_FOUND);
    }
}
