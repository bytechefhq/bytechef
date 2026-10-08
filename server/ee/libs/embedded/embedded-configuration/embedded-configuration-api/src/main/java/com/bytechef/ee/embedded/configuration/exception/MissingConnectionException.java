/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.exception;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class MissingConnectionException extends RuntimeException {

    private final String componentName;

    public MissingConnectionException(String componentName) {
        super("No connection found for component: " + componentName);

        this.componentName = componentName;
    }

    public String getComponentName() {
        return componentName;
    }
}
