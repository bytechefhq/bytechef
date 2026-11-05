/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.exception;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TitleGenerationFailedException extends RuntimeException {

    public TitleGenerationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
