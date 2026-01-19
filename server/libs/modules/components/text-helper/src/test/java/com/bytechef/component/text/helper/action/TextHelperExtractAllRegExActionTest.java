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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class TextHelperExtractAllRegExActionTest {

    @Test
    void testPerformSingleMatch() {
        String input = "My email is test@example.com";
        String regex = "[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}";

        Parameters mockedParameters = MockParametersFactory.create(Map.of(TEXT, input, REGULAR_EXPRESSION, regex));

        List<String> result = TextHelperExtractAllRegExAction.perform(mockedParameters, null, null);

        assertEquals(List.of("test@example.com"), result);
    }

    @Test
    void testPerformMultipleMatches() {
        String input = "Contact: test@example.com or admin@site.org";
        String regex = "[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}";

        Parameters mockedParameters = MockParametersFactory.create(Map.of(TEXT, input, REGULAR_EXPRESSION, regex));

        List<String> result = TextHelperExtractAllRegExAction.perform(mockedParameters, null, null);

        assertEquals(2, result.size());
        assertEquals(List.of("test@example.com", "admin@site.org"), result);
    }

    @Test
    void testPerformNoMatch() {
        String input = "No emails here!";
        String regex = "[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}";

        Parameters mockedParameters = MockParametersFactory.create(Map.of(TEXT, input, REGULAR_EXPRESSION, regex));

        List<String> result = TextHelperExtractAllRegExAction.perform(mockedParameters, null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testExtractMatchesSpecialCharacters() {
        String input = "Symbols: $var1 #tag @user";
        String regex = "[$#@]\\w+";

        Parameters mockedParameters = MockParametersFactory.create(Map.of(TEXT, input, REGULAR_EXPRESSION, regex));

        List<String> result = TextHelperExtractAllRegExAction.perform(mockedParameters, null, null);

        assertEquals(3, result.size());
        assertEquals(List.of("$var1", "#tag", "@user"), result);
    }

    @Test
    void testPerformCaptureGroups() {
        String input = "He said \"Hello\" and she said \"Hi\"";
        String regex = "\"([^\"]*)\"";

        Parameters mockedParameters = MockParametersFactory.create(Map.of(TEXT, input, REGULAR_EXPRESSION, regex));

        List<String> result = TextHelperExtractAllRegExAction.perform(mockedParameters, null, null);

        assertEquals(2, result.size());
        assertEquals(List.of("\"Hello\"", "\"Hi\""), result);
    }
}
