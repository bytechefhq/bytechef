/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.deployment.vercel;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class VercelDeploymentException extends RuntimeException {

    public VercelDeploymentException(String message) {
        super(message);
    }
}
