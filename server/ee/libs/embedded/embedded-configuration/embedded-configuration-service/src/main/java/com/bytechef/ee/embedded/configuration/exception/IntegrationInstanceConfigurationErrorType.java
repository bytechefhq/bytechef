/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.exception;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.exception.AbstractErrorType;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class IntegrationInstanceConfigurationErrorType extends AbstractErrorType {

    public static final IntegrationInstanceConfigurationErrorType CREATE_INTEGRATION_INSTANCE_CONFIGURATION =
        new IntegrationInstanceConfigurationErrorType(100);
    public static final IntegrationInstanceConfigurationErrorType REQUIRED_WORKFLOW_CONNECTIONS =
        new IntegrationInstanceConfigurationErrorType(101);
    public static final IntegrationInstanceConfigurationErrorType INTEGRATION_INSTANCE_CONFIGURATION_NOT_FOUND =
        new IntegrationInstanceConfigurationErrorType(102);

    private IntegrationInstanceConfigurationErrorType(int errorKey) {
        super(IntegrationInstanceConfiguration.class, errorKey);
    }
}
