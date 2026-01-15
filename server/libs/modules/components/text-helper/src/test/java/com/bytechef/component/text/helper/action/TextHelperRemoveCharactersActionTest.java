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

import static com.bytechef.component.text.helper.constant.TextHelperConstants.CHARACTER;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class TextHelperRemoveCharactersActionTest {

    private static String run(String input, String charactersToRemove) {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(TEXT, input, CHARACTER, charactersToRemove));

        return TextHelperRemoveCharactersAction.perform(mockedParameters, null, null);
    }

    @Test
    void removesSpecifiedCharacters() {
        String input = "Hello, World!";
        String result = run(input, "!");

        assertEquals("Hello, World", result);
    }

    @Test
    void doesNothingWhenNoCharactersMatch() {
        String input = "HelloWorld";
        String result = run(input, "l");

        assertEquals("HeoWord", result);
    }

    @Test
    void removesRepeatedCharacters() {
        String input = "a-b-c-d";
        String result = run(input, "-");

        assertEquals("abcd", result);
    }

    @Test
    void handlesEmptyInputString() {
        String input = "";
        String result = run(input, "a");

        assertEquals("", result);
    }

    @Test
    void handlesEmptyCharactersToRemove() {
        String input = "Hello";
        String result = run(input, "");

        assertEquals("Hello", result);
    }
}
