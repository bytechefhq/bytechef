/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.domain;

/**
 * @version ee
 *
 * @author Ivica Cardic
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

    private static final java.util.List<AiGatewayProviderType> VALUES = java.util.List.of(values());

    public static AiGatewayProviderType fromOrdinal(int ordinal) {
        return VALUES.get(ordinal);
    }
}
