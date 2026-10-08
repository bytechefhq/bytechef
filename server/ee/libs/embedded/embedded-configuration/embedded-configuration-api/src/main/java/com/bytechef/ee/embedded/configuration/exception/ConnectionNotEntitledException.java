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
public class ConnectionNotEntitledException extends RuntimeException {

    private final String componentName;
    private final long connectionId;

    public ConnectionNotEntitledException(String componentName, long connectionId) {
        super("Connection %s is not a %s connection of this connected user".formatted(connectionId, componentName));

        this.componentName = componentName;
        this.connectionId = connectionId;
    }

    public String getComponentName() {
        return componentName;
    }

    public long getConnectionId() {
        return connectionId;
    }
}
