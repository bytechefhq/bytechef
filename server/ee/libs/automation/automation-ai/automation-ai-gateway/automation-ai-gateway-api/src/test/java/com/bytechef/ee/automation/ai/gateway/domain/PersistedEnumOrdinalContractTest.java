/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayBudgetEnforcementMode;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PersistedEnumOrdinalContractTest {

    @Test
    void testAiGatewayProviderTypeOrdinalContract() {
        assertThat(AiGatewayProviderType.values()).containsExactly(
            AiGatewayProviderType.ANTHROPIC,
            AiGatewayProviderType.AZURE_OPENAI,
            AiGatewayProviderType.COHERE,
            AiGatewayProviderType.DEEPSEEK,
            AiGatewayProviderType.GOOGLE_GEMINI,
            AiGatewayProviderType.GROQ,
            AiGatewayProviderType.MISTRAL,
            AiGatewayProviderType.OPENAI);
    }

    @Test
    void testAiGatewayBudgetEnforcementModeOrdinalContract() {
        assertThat(AiGatewayBudgetEnforcementMode.values()).containsExactly(
            AiGatewayBudgetEnforcementMode.HARD,
            AiGatewayBudgetEnforcementMode.SOFT);
    }
}
