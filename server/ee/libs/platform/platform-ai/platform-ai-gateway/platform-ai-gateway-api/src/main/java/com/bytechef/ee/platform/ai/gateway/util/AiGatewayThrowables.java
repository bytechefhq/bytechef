/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.util;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiGatewayThrowables {

    public static final int DEFAULT_MAX_RENDERED_LENGTH = 1024;

    private AiGatewayThrowables() {
    }

    public static String summarize(Throwable throwable) {
        return summarize(throwable, DEFAULT_MAX_RENDERED_LENGTH);
    }

    public static String summarize(Throwable throwable, int maxLength) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException("maxLength must be > 0; got " + maxLength);
        }

        String name = throwable.getClass()
            .getSimpleName();
        String message = mostSpecificMessage(throwable);

        String rendered = message == null ? name : name + ": " + message;

        return rendered.length() <= maxLength
            ? rendered
            : rendered.substring(0, maxLength);
    }

    private static String mostSpecificMessage(Throwable throwable) {
        Throwable current = throwable;

        for (int depth = 0; depth < 6 && current != null; depth++) {
            String message = current.getMessage();

            if (message != null && !message.isBlank()) {
                if (current == throwable) {
                    return message;
                }

                return message + " (root cause: " + current.getClass()
                    .getSimpleName() + ")";
            }

            Throwable next = current.getCause();

            if (next == current) {
                break;
            }

            current = next;
        }

        return null;
    }
}
