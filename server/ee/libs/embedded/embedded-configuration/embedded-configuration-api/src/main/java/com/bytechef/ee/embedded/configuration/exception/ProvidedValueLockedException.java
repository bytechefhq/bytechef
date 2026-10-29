/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.exception;

/**
 * Thrown when a connected user tries to set a connection or an input value that the configuration of the automation
 * workflow project provides for every connected user.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ProvidedValueLockedException extends RuntimeException {

    private final Kind kind;
    private final String name;

    public ProvidedValueLockedException(String name, Kind kind) {
        super(
            (kind == Kind.CONNECTION
                ? "The connection of component %s is provided by the configuration"
                : "The value of input %s is provided by the configuration").formatted(name));

        this.kind = kind;
        this.name = name;
    }

    public Kind getKind() {
        return kind;
    }

    /**
     * Returns the component name of the connection slot, or the input name, the configuration provides.
     */
    public String getName() {
        return name;
    }

    public enum Kind {
        CONNECTION, INPUT
    }
}
