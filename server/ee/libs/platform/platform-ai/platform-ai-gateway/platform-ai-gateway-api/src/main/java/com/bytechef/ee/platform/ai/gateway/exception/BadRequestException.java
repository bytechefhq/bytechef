/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.exception;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
public final class BadRequestException extends RuntimeException {

    private final SafeMessage safeClientMessage;

    public BadRequestException(SafeMessage safeClientMessage, String internalDetail) {
        this(safeClientMessage, internalDetail, null);
    }

    public BadRequestException(SafeMessage safeClientMessage, String internalDetail, Throwable cause) {
        super(internalDetail, cause);

        if (safeClientMessage == null) {
            throw new IllegalArgumentException("safeClientMessage must not be null");
        }

        this.safeClientMessage = safeClientMessage;
    }

    public String getSafeClientMessage() {
        return safeClientMessage.value();
    }
}
