/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.property;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 * @author Ivica Cardic
 */
public record PropertyCopilotResult(String value, boolean valid, @Nullable String message) {

    public PropertyCopilotResult {
        Objects.requireNonNull(value, "value");

        if (valid && message != null) {
            throw new IllegalArgumentException("A valid result must not carry a message");
        }

        if (!valid && (message == null || message.isBlank())) {
            throw new IllegalArgumentException("An invalid result must carry a message");
        }
    }

    public static PropertyCopilotResult valid(String value) {
        return new PropertyCopilotResult(value, true, null);
    }

    public static PropertyCopilotResult invalid(String value, String message) {
        return new PropertyCopilotResult(value, false, message);
    }
}
