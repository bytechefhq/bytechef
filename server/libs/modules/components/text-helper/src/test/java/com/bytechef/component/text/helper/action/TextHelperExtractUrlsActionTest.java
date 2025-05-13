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

package com.bytechef.component.text.helper.action;

import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class TextHelperExtractUrlsActionTest {

    @Test
    void testPerform() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(TEXT,
                "Check out these websites: https://www.example.com and http://blog.example.org. For more information, visit https://docs.example.net/guide or www.example.io."));

        List<String> result = TextHelperExtractUrlsAction.perform(parameters, parameters, mock(ActionContext.class));

        assertEquals(
            List.of(
                "https://www.example.com", "http://blog.example.org", "https://docs.example.net/guide",
                "www.example.io"),
            result);
    }

    @Test
    public void testPerformWithNoUrls() {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(TEXT, "\"No URLs here!\""));

        List<String> result = TextHelperExtractUrlsAction.perform(mockedParameters, null, mock(ActionContext.class));

        assertTrue(result.isEmpty());
    }

}
