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

package com.bytechef.component.ai.llm.router.open.router.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SIZE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER;
import static com.bytechef.component.ai.llm.router.constant.RouterConstants.ASPECT_RATIO;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bytechef.component.ai.llm.router.open.router.model.OpenRouterImageModel;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OpenRouterCreateImageActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(MODEL, "model", SIZE, "1024x1024", ASPECT_RATIO, "1:1", USER, "user"));

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of(TOKEN, "token"));

    @Test
    void testCreateImageModel() {
        OpenRouterImageModel openRouterImageModel =
            (OpenRouterImageModel) OpenRouterCreateImageAction.IMAGE_MODEL.createImageModel(
                mockedParameters, mockedConnectionParameters);

        assertNotNull(openRouterImageModel);

        assertEquals("model", openRouterImageModel.getModel());
        assertEquals("1024x1024", openRouterImageModel.getSize());
        assertEquals("1:1", openRouterImageModel.getAspectRatio());
        assertEquals("user", openRouterImageModel.getUser());
    }
}
