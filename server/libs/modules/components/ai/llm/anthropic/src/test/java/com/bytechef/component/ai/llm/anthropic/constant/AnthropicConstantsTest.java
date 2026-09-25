/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.ai.llm.anthropic.constant;

import static com.bytechef.component.ai.llm.anthropic.constant.AnthropicConstants.ASK_PROPERTIES;
import static com.bytechef.component.ai.llm.anthropic.constant.AnthropicConstants.MAX_TOKENS_PROPERTY;
import static com.bytechef.component.ai.llm.anthropic.constant.AnthropicConstants.TEMPERATURE_PROPERTY;
import static com.bytechef.component.ai.llm.anthropic.constant.AnthropicConstants.TOP_K_PROPERTY;
import static com.bytechef.component.ai.llm.anthropic.constant.AnthropicConstants.TOP_P_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.ai.llm.constant.LLMConstants;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AnthropicConstantsTest {

    @Test
    void testSamplingPropertiesHaveNoDefaultValue() {
        assertTrue(TOP_K_PROPERTY.getDefaultValue()
            .isEmpty());
        assertTrue(TEMPERATURE_PROPERTY.getDefaultValue()
            .isEmpty());
        assertTrue(TOP_P_PROPERTY.getDefaultValue()
            .isEmpty());
    }

    @Test
    void testAskPropertiesUseTheAnthropicTopKProperty() {
        assertTrue(ASK_PROPERTIES.contains(TOP_K_PROPERTY));
        assertFalse(ASK_PROPERTIES.contains(LLMConstants.TOP_K_PROPERTY));
    }

    @Test
    void testMaxTokensPropertyDefaultStaysWithinTheOutputCap() {
        assertEquals(Optional.of(16000L), MAX_TOKENS_PROPERTY.getDefaultValue());
        assertEquals(Optional.of(1L), MAX_TOKENS_PROPERTY.getMinValue());
        assertEquals(Optional.of(128000L), MAX_TOKENS_PROPERTY.getMaxValue());
    }
}
