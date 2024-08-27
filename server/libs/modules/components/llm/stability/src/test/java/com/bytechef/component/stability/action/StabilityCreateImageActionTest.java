/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.stability.action;

import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.llm.test.ImageActionTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.stabilityai.StabilityAiImageModel;

class StabilityCreateImageActionTest extends ImageActionTest {
    @Test
    void testPerform() {
        performTest(
            (ActionDefinition.SingleConnectionPerformFunction) StabilityCreateImageAction.ACTION_DEFINITION.getPerform()
                .get());
    }

    @Test
    void testGetResponse() {
        getResponseTest(mock(StabilityAiImageModel.class));
    }
}
