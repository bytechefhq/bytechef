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

package com.bytechef.component.ai.universal.image;

import static com.bytechef.platform.ai.llm.Provider.ANTHROPIC;
import static com.bytechef.platform.ai.llm.Provider.AZURE_OPEN_AI;
import static com.bytechef.platform.ai.llm.Provider.OPEN_AI;
import static com.bytechef.platform.ai.llm.Provider.STABILITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.ai.llm.ImageModel;
import com.bytechef.component.ai.llm.LLMModelRegistry;
import com.bytechef.component.ai.llm.azure.openai.action.AzureOpenAiCreateImageAction;
import com.bytechef.component.ai.llm.openai.action.OpenAiCreateImageAction;
import com.bytechef.component.ai.llm.stability.action.StabilityCreateImageAction;
import com.bytechef.platform.ai.llm.Provider;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class LLMImageProviderRegistryTest {

    private static final Map<Provider, ImageModel> SWITCH_IMAGE_MODELS = new EnumMap<>(Map.of(
        AZURE_OPEN_AI, AzureOpenAiCreateImageAction.IMAGE_MODEL,
        OPEN_AI, OpenAiCreateImageAction.IMAGE_MODEL,
        STABILITY, StabilityCreateImageAction.IMAGE_MODEL));

    @Test
    void testAllImageProvidersResolveToAnImageModel() {
        List<Provider> imageProviders = List.of(AZURE_OPEN_AI, OPEN_AI, STABILITY);

        for (Provider provider : imageProviders) {
            assertNotNull(LLMModelRegistry.getImageModel(provider), "No image model for " + provider);
        }
    }

    @Test
    void testEveryProviderResolvesToTheImageModelOfTheReplacedSwitch() {
        for (Provider provider : Provider.values()) {
            ImageModel expectedImageModel = SWITCH_IMAGE_MODELS.get(provider);

            assertEquals(expectedImageModel != null, LLMModelRegistry.hasImageModel(provider), provider.name());

            if (expectedImageModel == null) {
                assertThrows(IllegalArgumentException.class, () -> LLMModelRegistry.getImageModel(provider));
            } else {
                assertSame(expectedImageModel, LLMModelRegistry.getImageModel(provider), provider.name());
            }
        }
    }

    @Test
    void testHasImageModelReflectsImageCapability() {
        assertTrue(LLMModelRegistry.hasImageModel(OPEN_AI));
        assertFalse(LLMModelRegistry.hasImageModel(ANTHROPIC));
    }
}
