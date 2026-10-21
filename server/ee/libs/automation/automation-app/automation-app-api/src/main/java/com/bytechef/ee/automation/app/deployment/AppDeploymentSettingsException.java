/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.deployment;

/**
 * Thrown by {@link AppDeploymentProvider#validateDeploymentSettings} when deployment settings cannot be used. The
 * message is shown to the user, so it names the offending settings keys and never their values.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AppDeploymentSettingsException extends RuntimeException {

    public AppDeploymentSettingsException(String message) {
        super(message);
    }
}
