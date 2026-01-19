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

package com.bytechef.component.text.helper.util;

import static com.bytechef.component.text.helper.util.TextHelperUtils.getPatternEndIndex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.exception.ProviderException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class TextHelperUtilsTest {

    @Test
    void testGetPatternEndIndexNormalCase() {
        String text = "Hello world! This is a sample text. Hello again!";
        String pattern = "Hello";
        int matchNumber = 2;

        int result = getPatternEndIndex(pattern, text, matchNumber);
        assertEquals(41, result);
    }

    @Test
    void testGetPatternIndexPatternEndNotFound() {
        String text = "Hello world!";
        String pattern = "Hi";
        int matchNumber = 1;

        assertThrows(ProviderException.class, () -> getPatternEndIndex(pattern, text, matchNumber));
    }

    @Test
    void testExtractByRegExSingleMatch() {
        String input = "My email is test@example.com";
        String regex = "[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}";

        List<String> matches = TextHelperUtils.extractByRegEx(input, regex);

        assertEquals(1, matches.size());
        assertEquals("test@example.com", matches.get(0));
    }

    @Test
    void testExtractByRegExMultipleMatches() {
        String input = "Contact: test@example.com or admin@site.org";
        String regex = "[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}";

        List<String> matches = TextHelperUtils.extractByRegEx(input, regex);

        assertEquals(2, matches.size());
        assertEquals("test@example.com", matches.get(0));
        assertEquals("admin@site.org", matches.get(1));
    }

    @Test
    void testExtractByRegExNoMatch() {
        String input = "No emails here!";
        String regex = "[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}";

        List<String> matches = TextHelperUtils.extractByRegEx(input, regex);

        assertTrue(matches.isEmpty());
    }

    @Test
    void testExtractByRegExSpecialCharacters() {
        String input = "Symbols: $var1 #tag @user";
        String regex = "[$#@]\\w+";

        List<String> matches = TextHelperUtils.extractByRegEx(input, regex);

        assertEquals(3, matches.size());
        assertEquals("$var1", matches.get(0));
        assertEquals("#tag", matches.get(1));
        assertEquals("@user", matches.get(2));
    }

    @Test
    void testExtractByRegExCaptureGroups() {
        String input = "He said \"Hello\" and she said \"Hi\"";
        String regex = "\"([^\"]*)\"";

        List<String> matches = TextHelperUtils.extractByRegEx(input, regex);

        assertEquals(2, matches.size());
        assertEquals("\"Hello\"", matches.get(0));
        assertEquals("\"Hi\"", matches.get(1));
    }
}
