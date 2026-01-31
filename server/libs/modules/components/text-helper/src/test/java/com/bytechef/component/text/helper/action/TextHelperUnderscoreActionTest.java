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

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class TextHelperUnderscoreActionTest {

    private static String run(String input) {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(TEXT, input));

        return TextHelperUnderscoreAction.perform(mockedParameters, null, null);
    }

    @Test
    void convertsBasicSentence() {
        String input = "Hello, World!";
        String expected = "hello_world";

        assertEquals(expected, run(input));
    }

    @Test
    void removesSpecialCharacters() {
        String input = "Java@#$ Is *** Awesome!!!";
        String expected = "java_is_awesome";

        assertEquals(expected, run(input));
    }

    @Test
    void handlesMultipleSpaces() {
        String input = "  Too   many   spaces  ";
        String expected = "too_many_spaces";

        assertEquals(expected, run(input));
    }

    @Test
    void keepsNumbers() {
        String input = "Version 2 Release 10";
        String expected = "version_2_release_10";

        assertEquals(expected, run(input));
    }
}
