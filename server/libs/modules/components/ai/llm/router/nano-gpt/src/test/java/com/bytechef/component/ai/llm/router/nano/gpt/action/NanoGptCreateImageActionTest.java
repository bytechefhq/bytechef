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

package com.bytechef.component.ai.llm.router.nano.gpt.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.N;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SIZE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER;
import static com.bytechef.component.ai.llm.router.nano.gpt.constant.NanoGptConstants.GUIDANCE_SCALE;
import static com.bytechef.component.ai.llm.router.nano.gpt.constant.NanoGptConstants.NUM_INFERENCE_STEPS;
import static com.bytechef.component.ai.llm.router.nano.gpt.constant.NanoGptConstants.STRENGTH;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bytechef.component.ai.llm.router.nano.gpt.model.NanoGptImageModel;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class NanoGptCreateImageActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.ofEntries(Map.entry(MODEL, "model"), Map.entry(SIZE, "1024x1024"),
            Map.entry(RESPONSE_FORMAT, "url"), Map.entry(N, 1), Map.entry(SEED, 1),
            Map.entry(GUIDANCE_SCALE, 0.0), Map.entry(USER, "user"), Map.entry(STRENGTH, 0.0),
            Map.entry(NUM_INFERENCE_STEPS, 1)));

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of(TOKEN, "token"));

    @Test
    void testCreateImageModel() {
        NanoGptImageModel nanoGptImageModel =
            (NanoGptImageModel) NanoGptCreateImageAction.IMAGE_MODEL.createImageModel(
                mockedParameters, mockedConnectionParameters);

        assertNotNull(nanoGptImageModel);

        assertEquals("model", nanoGptImageModel.getModel());
        assertEquals("1024x1024", nanoGptImageModel.getSize());
        assertEquals("url", nanoGptImageModel.getResponseFormat());
        assertEquals(1, nanoGptImageModel.getN());
        assertEquals(1, nanoGptImageModel.getSeed());
        assertEquals(0.0, nanoGptImageModel.getGuidanceScale());
        assertEquals(0.0, nanoGptImageModel.getStrength());
        assertEquals(1, nanoGptImageModel.getNumInferenceSteps());
        assertEquals("user", nanoGptImageModel.getUser());
    }
}
