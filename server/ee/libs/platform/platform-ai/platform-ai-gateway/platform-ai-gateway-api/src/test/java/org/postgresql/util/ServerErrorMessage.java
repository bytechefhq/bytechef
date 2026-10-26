/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package org.postgresql.util;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ServerErrorMessage {

    private final String constraint;

    public ServerErrorMessage(String constraint) {
        this.constraint = constraint;
    }

    public String getConstraint() {
        return constraint;
    }
}
