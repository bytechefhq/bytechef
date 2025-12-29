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

import static com.bytechef.component.text.helper.constant.TextHelperConstants.ESCAPE_CHARACTERS;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class TextHelperEscapeCharactersActionTest {

    static final Context mockedContext = mock(Context.class);

    @Test
    void testPerformSingleCharacterEscape() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TEXT, "abc", ESCAPE_CHARACTERS, List.of("a")));

        String result = TextHelperEscapeCharactersAction.perform(
            mockedParameters, null, mockedContext);

        assertEquals("\\abc", result);
    }

    @Test
    void testPerformMultipleCharactersEscape() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TEXT, "Hello (World)?", ESCAPE_CHARACTERS, List.of("(", ")", "?")));

        String result = TextHelperEscapeCharactersAction.perform(
            mockedParameters, null, mockedContext);

        assertEquals("Hello \\(World\\)\\?", result);
    }

    @Test
    void testPerformCharacterNotInTextNoChange() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TEXT, "dog", ESCAPE_CHARACTERS, List.of("x")));

        String result = TextHelperEscapeCharactersAction.perform(
            mockedParameters, null, mockedContext);

        assertEquals("dog", result);
    }

    @Test
    void testPerformEscapingSlashCharacter() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TEXT, "C:\\Temp", ESCAPE_CHARACTERS, List.of("\\")));

        String result = TextHelperEscapeCharactersAction.perform(
            mockedParameters, null, mockedContext);

        assertEquals("C:\\\\Temp", result);
    }

    @Test
    void testPerformEscapeNewLine() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TEXT, "Happy \nNew Year!", ESCAPE_CHARACTERS, List.of("\n")));

        String result = TextHelperEscapeCharactersAction.perform(
            mockedParameters, null, mockedContext);

        assertEquals("Happy \\\nNew Year!", result);
    }
}
