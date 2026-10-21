/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.deployment.hook;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class DeployHookDeploymentException extends RuntimeException {

    public DeployHookDeploymentException(String message) {
        super(message);
    }
}
