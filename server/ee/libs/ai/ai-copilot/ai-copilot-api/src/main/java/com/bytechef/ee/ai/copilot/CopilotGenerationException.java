/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot;

/**
 * @version ee
 * @author Ivica Cardic
 */
public class CopilotGenerationException extends RuntimeException {

    public CopilotGenerationException(String message) {
        super(message);
    }
}
