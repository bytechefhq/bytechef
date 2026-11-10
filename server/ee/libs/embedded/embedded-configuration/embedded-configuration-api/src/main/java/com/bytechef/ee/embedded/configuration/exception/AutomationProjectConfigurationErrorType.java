/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.exception;

import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.exception.AbstractErrorType;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AutomationProjectConfigurationErrorType extends AbstractErrorType {

    public static final AutomationProjectConfigurationErrorType PROJECT_VERSION_NOT_PUBLISHED =
        new AutomationProjectConfigurationErrorType(100);

    public static final AutomationProjectConfigurationErrorType UNKNOWN_ENVIRONMENT =
        new AutomationProjectConfigurationErrorType(101);

    public static final AutomationProjectConfigurationErrorType VERSION_LACKS_REFERENCED_WORKFLOWS =
        new AutomationProjectConfigurationErrorType(103);

    public static final AutomationProjectConfigurationErrorType INVALID_PROVIDED_VALUE =
        new AutomationProjectConfigurationErrorType(104);

    public static final AutomationProjectConfigurationErrorType CONFIGURATION_NAME_EXISTS =
        new AutomationProjectConfigurationErrorType(105);

    public static final AutomationProjectConfigurationErrorType CONFIGURATION_PROJECT_MISMATCH =
        new AutomationProjectConfigurationErrorType(106);

    public static final AutomationProjectConfigurationErrorType CONFIGURATION_IN_USE =
        new AutomationProjectConfigurationErrorType(107);

    private AutomationProjectConfigurationErrorType(int errorKey) {
        super(AutomationProjectConfiguration.class, errorKey);
    }
}
