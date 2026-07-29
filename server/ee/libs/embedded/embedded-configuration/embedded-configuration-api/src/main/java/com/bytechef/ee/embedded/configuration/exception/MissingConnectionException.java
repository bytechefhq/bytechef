/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.exception;

/**
 * Thrown when a reference cannot be auto-wired because a component it uses has no matching connection for the connected
 * user. Deliberately NOT an {@code AbstractException} subtype: those all map to HTTP 400 through
 * {@code GlobalResponseEntityExceptionHandler}, and this condition is a 409 (the reference is left in place, disabled,
 * so the caller can create the connection and retry without redoing provisioning).
 *
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
