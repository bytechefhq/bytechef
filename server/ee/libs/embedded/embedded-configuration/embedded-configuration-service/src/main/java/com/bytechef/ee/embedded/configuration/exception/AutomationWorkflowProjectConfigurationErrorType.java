/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.exception;

import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import com.bytechef.exception.AbstractErrorType;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AutomationWorkflowProjectConfigurationErrorType extends AbstractErrorType {

    public static final AutomationWorkflowProjectConfigurationErrorType PROJECT_VERSION_NOT_PUBLISHED =
        new AutomationWorkflowProjectConfigurationErrorType(100);

    private AutomationWorkflowProjectConfigurationErrorType(int errorKey) {
        super(AutomationWorkflowProjectConfiguration.class, errorKey);
    }
}
