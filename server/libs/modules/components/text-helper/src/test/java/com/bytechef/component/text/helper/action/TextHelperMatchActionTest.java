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
class TextHelperMatchActionTest {

    private List<String> run(String text, String regex) {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(TEXT, text, REGULAR_EXPRESSION, regex));

        return TextHelperMatchAction.perform(mockedParameters, null, null);
    }

    @Test
    void shouldReturnAllEmailMatches() {
        String input = "My email is test@example.com and backup hello@domain.org";
        String regex = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";

        List<String> matches = run(input, regex);

        assertEquals(2, matches.size());
        assertTrue(matches.contains("test@example.com"));
        assertTrue(matches.contains("hello@domain.org"));
    }

    @Test
    void shouldReturnEmptyListIfNoMatch() {
        String input = "No emails here!";
        String regex = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";

        List<String> matches = run(input, regex);

        assertTrue(matches.isEmpty());
    }

    @Test
    void shouldHandleEmptyInput() {
        String input = "";
        String regex = "\\d+";

        List<String> matches = run(input, regex);

        assertTrue(matches.isEmpty());
    }

    @Test
    void shouldReturnAllNumbers() {
        String input = "Numbers: 42, 123, 7";
        String regex = "\\d+";

        List<String> matches = run(input, regex);

        assertEquals(3, matches.size());
        assertTrue(matches.contains("42"));
        assertTrue(matches.contains("123"));
        assertTrue(matches.contains("7"));
    }
}
