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
public class PSQLException extends RuntimeException {

    private final ServerErrorMessage serverErrorMessage;

    public PSQLException(String message, ServerErrorMessage serverErrorMessage) {
        super(message);
        this.serverErrorMessage = serverErrorMessage;
    }

    public ServerErrorMessage getServerErrorMessage() {
        return serverErrorMessage;
    }
}
