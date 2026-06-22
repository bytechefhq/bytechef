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

package com.bytechef.component.ai.universal.image.util;

import static com.bytechef.platform.ai.llm.Provider.ANTHROPIC;
import static com.bytechef.platform.ai.llm.Provider.OPEN_AI;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.config.ApplicationProperties.Ai;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AiImageUtilsTest {

    @Test
    void testIsSelectableWhenConfiguredApiKeyPresent() {
        Ai.Provider aiProvider = new Ai.Provider();

        aiProvider.getOpenAi()
            .setApiKey("sk-open-ai");

        assertTrue(AiImageUtils.isSelectable(OPEN_AI, aiProvider, List.of()));
    }

    @Test
    void testIsSelectableWhenActiveProviderKeyPresent() {
        assertTrue(AiImageUtils.isSelectable(OPEN_AI, new Ai.Provider(), List.of(OPEN_AI.getKey())));
    }

    @Test
    void testNotSelectableWhenImageProviderHasNoKey() {
        assertFalse(AiImageUtils.isSelectable(OPEN_AI, new Ai.Provider(), List.of()));
    }

    @Test
    void testNotSelectableForNonImageProviderEvenWithKey() {
        Ai.Provider aiProvider = new Ai.Provider();

        aiProvider.getAnthropic()
            .setApiKey("sk-anthropic");

        assertFalse(AiImageUtils.isSelectable(ANTHROPIC, aiProvider, List.of(ANTHROPIC.getKey())));
    }
}
