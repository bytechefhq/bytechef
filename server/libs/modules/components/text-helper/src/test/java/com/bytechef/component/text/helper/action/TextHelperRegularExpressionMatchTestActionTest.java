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

import static com.bytechef.component.text.helper.constant.TextHelperConstants.IGNORE_CASE;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.MULTILINE;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.REGULAR_EXPRESSION;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.UNICODE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class TextHelperRegularExpressionMatchTestActionTest {

    private static boolean run(String text, String regEx, boolean ignoreCase, boolean multiline, boolean unicode) {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                TEXT, text,
                REGULAR_EXPRESSION, regEx,
                IGNORE_CASE, ignoreCase,
                MULTILINE, multiline,
                UNICODE, unicode));

        return TextHelperRegularExpressionMatchTestAction.perform(mockedParameters, null, null);
    }

    @Test
    void matchesSimplePattern() {
        String text = "Hello 123";
        String regex = "[A-Za-z]+\\s\\d+";

        assertTrue(run(text, regex, false, false, false));
    }

    @Test
    void doesNotMatchWhenPatternAbsent() {
        String text = "No numbers here!";
        String regex = "[A-Za-z]+\\s\\d+";

        assertFalse(run(text, regex, false, false, false));
    }

    @Test
    void ignoresCaseWhenFlagSet() {
        String text = "HELLO 456";
        String regex = "hello\\s\\d+";

        assertTrue(run(text, regex, true, false, false));
    }

    @Test
    void respectsCaseWhenFlagNotSet() {
        String text = "HELLO 456";
        String regex = "hello\\s\\d+";

        assertFalse(run(text, regex, false, false, false));
    }

    @Test
    void matchesMultilineText() {
        String text = "Line1\nLine2 789";
        String regex = "Line2\\s\\d+";

        assertTrue(run(text, regex, false, true, false));
    }

    @Test
    void handlesEmptyText() {
        String text = "";
        String regex = ".*";

        assertTrue(run(text, regex, false, false, false));
    }

    @Test
    void matchesUnicodeWhenFlagSet() {
        String text = "Café 123";
        String regex = "\\w+\\s\\d+";

        assertTrue(run(text, regex, false, false, true));
    }
}
