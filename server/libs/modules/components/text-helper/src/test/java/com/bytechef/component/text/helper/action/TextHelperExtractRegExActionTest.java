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

import static com.bytechef.component.text.helper.constant.TextHelperConstants.REGULAR_EXPRESSION;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class TextHelperExtractRegExActionTest {

    @Test
    void testPerformSingleMatch() {
        String input = "My email is test@example.com";
        String regex = "[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}";

        Parameters mockedParameters = MockParametersFactory.create(Map.of(TEXT, input, REGULAR_EXPRESSION, regex));

        String result = TextHelperExtractRegExAction.perform(mockedParameters, null, null);

        assertEquals("test@example.com", result);
    }

    @Test
    void testPerformMultipleMatches() {
        String input = "Contact: test@example.com or admin@site.org";
        String regex = "[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}";

        Parameters mockedParameters = MockParametersFactory.create(Map.of(TEXT, input, REGULAR_EXPRESSION, regex));

        String result = TextHelperExtractRegExAction.perform(mockedParameters, null, null);

        assertEquals("test@example.com", result);
    }
}
