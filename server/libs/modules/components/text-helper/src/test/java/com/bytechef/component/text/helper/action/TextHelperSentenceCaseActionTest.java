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
class TextHelperSentenceCaseActionTest {

    private static String run(String input) {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(TEXT, input));

        return TextHelperSentenceCaseAction.perform(mockedParameters, null, null);
    }

    @Test
    void testSingleSentence() {
        String input = "hELLO world!";
        String expected = "Hello world!";

        assertEquals(expected, run(input));
    }

    @Test
    void testMultipleSentences() {
        String input = "hELLO world! this is JAVA. how are YOU?";
        String expected = "Hello world! This is java. How are you?";

        assertEquals(expected, run(input));
    }

    @Test
    void testEmptyString() {
        String input = "";
        String expected = "";

        assertEquals(expected, run(input));
    }

    @Test
    void testSingleWord() {
        String input = "hELLO";
        String expected = "Hello";

        assertEquals(expected, run(input));
    }

    @Test
    void testSentenceEndingWithDifferentPunctuations() {
        String input = "how are you? i am fine! good.";
        String expected = "How are you? I am fine! Good.";

        assertEquals(expected, run(input));
    }

    @Test
    void testAlreadyProperlyCased() {
        String input = "This is already properly cased.";
        String expected = "This is already properly cased.";

        assertEquals(expected, run(input));
    }

    @Test
    void testMixedCasingAndSpaces() {
        String input = "  tHIS is a TeST. another SENTENCE? yes!  ";
        String expected = "This is a test. Another sentence? Yes!";

        assertEquals(expected, run(input));
    }
}
