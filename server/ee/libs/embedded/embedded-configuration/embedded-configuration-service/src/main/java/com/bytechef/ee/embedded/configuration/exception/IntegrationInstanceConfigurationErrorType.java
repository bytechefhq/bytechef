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

    public static final IntegrationInstanceConfigurationErrorType INTEGRATION_NOT_PUBLISHED =
        new IntegrationInstanceConfigurationErrorType(100);
    public static final IntegrationInstanceConfigurationErrorType WORKFLOW_CONNECTIONS_NOT_FOUND =
        new IntegrationInstanceConfigurationErrorType(101);
    public static final IntegrationInstanceConfigurationErrorType INSTANCE_CONFIGURATION_NOT_FOUND =
        new IntegrationInstanceConfigurationErrorType(102);
    public static final IntegrationInstanceConfigurationErrorType INVALID_INTEGRATION_VERSION =
        new IntegrationInstanceConfigurationErrorType(103);
    public static final IntegrationInstanceConfigurationErrorType INSTANCE_CONFIGURATION_DISABLED =
        new IntegrationInstanceConfigurationErrorType(105);
    public static final IntegrationInstanceConfigurationErrorType WORKFLOW_NOT_IN_CONFIGURATION =
        new IntegrationInstanceConfigurationErrorType(106);
    public static final IntegrationInstanceConfigurationErrorType INSTANCE_CONFIGURATION_MISMATCH =
        new IntegrationInstanceConfigurationErrorType(107);
    public static final IntegrationInstanceConfigurationErrorType INSTANCE_CONFIGURATION_MODIFIED =
        new IntegrationInstanceConfigurationErrorType(108);
    public static final IntegrationInstanceConfigurationErrorType UNKNOWN_CONNECTION_PARAMETER =
        new IntegrationInstanceConfigurationErrorType(109);
    public static final IntegrationInstanceConfigurationErrorType BLANK_CONNECTION_PARAMETER =
        new IntegrationInstanceConfigurationErrorType(110);

    private IntegrationInstanceConfigurationErrorType(int errorKey) {
        super(IntegrationInstanceConfiguration.class, errorKey);
    }
}
