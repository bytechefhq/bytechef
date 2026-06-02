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

package com.bytechef.component.ai.llm.stability.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.N;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STYLE;
import static com.bytechef.component.ai.llm.stability.constant.StabilityConstants.CFG_SCALE;
import static com.bytechef.component.ai.llm.stability.constant.StabilityConstants.CLIP_GUIDANCE_PRESET;
import static com.bytechef.component.ai.llm.stability.constant.StabilityConstants.HEIGHT;
import static com.bytechef.component.ai.llm.stability.constant.StabilityConstants.SAMPLER;
import static com.bytechef.component.ai.llm.stability.constant.StabilityConstants.STEPS;
import static com.bytechef.component.ai.llm.stability.constant.StabilityConstants.WIDTH;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.stabilityai.StabilityAiImageModel;
import org.springframework.ai.stabilityai.StyleEnum;
import org.springframework.ai.stabilityai.api.StabilityAiImageOptions;

/**
 * @author Nikolina Spehar
 */
class StabilityCreateImageActionTest {

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of(TOKEN, "TOKEN"));
    private final Parameters mockedInputParameters = MockParametersFactory.create(
        Map.ofEntries(Map.entry(MODEL, "stable-diffusion-v1-6"), Map.entry(HEIGHT, 512), Map.entry(WIDTH, 512),
            Map.entry(N, 1), Map.entry(RESPONSE_FORMAT, "url"), Map.entry(STYLE, StyleEnum.ENHANCE.name()),
            Map.entry(CFG_SCALE, 7.0f), Map.entry(CLIP_GUIDANCE_PRESET, "FAST_BLUE"),
            Map.entry(SAMPLER, "K_EULER"), Map.entry(STEPS, 30), Map.entry(SEED, 12345L)));

    @Test
    void testCreateImageModel() {
        org.springframework.ai.image.ImageModel imageModel = StabilityCreateImageAction.IMAGE_MODEL.createImageModel(
            mockedInputParameters, mockedConnectionParameters);

        assertNotNull(imageModel);
        assertInstanceOf(StabilityAiImageModel.class, imageModel);

        StabilityAiImageOptions stabilityAiImageOptions = ((StabilityAiImageModel) imageModel).getOptions();

        assertEquals("stable-diffusion-v1-6", stabilityAiImageOptions.getModel());
        assertEquals(7.0f, stabilityAiImageOptions.getCfgScale());
        assertEquals("FAST_BLUE", stabilityAiImageOptions.getClipGuidancePreset());
        assertEquals(512, stabilityAiImageOptions.getHeight());
        assertEquals(1, stabilityAiImageOptions.getN());
        assertEquals("url", stabilityAiImageOptions.getResponseFormat());
        assertEquals("K_EULER", stabilityAiImageOptions.getSampler());
        assertEquals(12345L, stabilityAiImageOptions.getSeed());
        assertEquals(30, stabilityAiImageOptions.getSteps());
        assertEquals(StyleEnum.ENHANCE.toString(), stabilityAiImageOptions.getStylePreset());
        assertEquals(512, stabilityAiImageOptions.getWidth());
    }
}
