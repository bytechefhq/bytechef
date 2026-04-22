/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

/**
 * @version ee
 */
public enum AiGatewayProviderType {
    ANTHROPIC,
    AZURE_OPENAI,
    COHERE,
    DEEPSEEK,
    GOOGLE_GEMINI,
    GROQ,
    MISTRAL,
    OPENAI;

    // APPEND-ONLY: enum values are persisted by ordinal (see project MEMORY.md). Reordering or deleting any entry
    // above will corrupt existing rows. Always add new providers at the end of this list.

    private static final java.util.List<AiGatewayProviderType> VALUES = java.util.List.of(values());

    /**
     * Returns the enum constant for the given ordinal without reallocating the backing array on every call, per the
     * {@code AiObservabilityTrace} convention in this codebase.
     *
     * @throws IndexOutOfBoundsException if {@code ordinal} is outside {@code [0, size)}
     */
    public static AiGatewayProviderType fromOrdinal(int ordinal) {
        return VALUES.get(ordinal);
    }
}
