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

package com.bytechef.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.config.ApplicationProperties.Ai.Provider;
import org.junit.jupiter.api.Test;

class AiProviderApiKeyTest {

    @Test
    void testGetProviderApiKeyReturnsConfiguredKey() {
        Provider provider = new Provider();

        provider.getOpenAi()
            .setApiKey("sk-open-ai");

        assertEquals("sk-open-ai", provider.getProviderApiKey("ai.provider.openAi"));
    }

    @Test
    void testGetProviderApiKeyReturnsConfiguredKeyWhenKeyDiffersFromFieldName() {
        Provider provider = new Provider();

        provider.getDeepSeek()
            .setApiKey("sk-deepseek");

        assertEquals("sk-deepseek", provider.getProviderApiKey("ai.provider.deepseek"));
    }

    @Test
    void testGetProviderApiKeyReturnsNullForUnknownKey() {
        Provider provider = new Provider();

        assertNull(provider.getProviderApiKey("ai.provider.unknown"));
    }
}
