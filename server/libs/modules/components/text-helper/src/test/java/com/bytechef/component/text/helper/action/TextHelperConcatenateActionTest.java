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

package com.bytechef.component.text.helper.action;

import static com.bytechef.component.text.helper.constant.TextHelperConstants.SEPARATOR;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
class TextHelperConcatenateActionTest {

    @Test
    void testPerformWithSeparator() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(TEXTS, List.of("apple", "banana", "orange"), SEPARATOR, ", "));

        String result = TextHelperConcatenateAction.perform(parameters, parameters, mock(ActionContext.class));

        String expected = "apple, banana, orange";

        assertEquals(expected, result);
    }

    @Test
    void testPerformWithoutSeparator() {
        Parameters parameters = MockParametersFactory.create(Map.of(TEXTS, List.of("apple", "banana", "orange")));

        String result = TextHelperConcatenateAction.perform(parameters, parameters, mock(ActionContext.class));

        String expected = "applebananaorange";

        assertEquals(expected, result);
    }
}
