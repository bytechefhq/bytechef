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

import static com.bytechef.component.text.helper.constant.TextHelperConstants.PATTERN_END;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.PATTERN_START;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class TextHelperGetTextBetweenActionTest {

    @Test
    void testExtractBetweenNormalCase() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                TEXT, "Hello world! This is a sample text. Hello again!", PATTERN_START, "Hello", PATTERN_END,
                "again"));

        String result = TextHelperGetTextBetweenAction.perform(mockedParameters, mockedParameters, null);

        assertEquals(" world! This is a sample text. Hello ", result);
    }

    @Test
    void testExtractBetweenMultipleMatches() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TEXT, "A [first] B [second] C [third]", PATTERN_START, "[", PATTERN_END, "]"));

        String result = TextHelperGetTextBetweenAction.perform(mockedParameters, mockedParameters, null);

        assertEquals("first", result);
    }

    @Test
    void testExtractBetweenStartPatternNotFound() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TEXT, "No patterns here!", PATTERN_START, "Hello", PATTERN_END, "world"));

        assertThrows(ProviderException.class,
            () -> TextHelperGetTextBetweenAction.perform(mockedParameters, mockedParameters, null));
    }

    @Test
    void testExtractBetweenEndPatternNotFound() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TEXT, "Hello world!", PATTERN_START, "Hello", PATTERN_END, "Universe"));

        assertThrows(ProviderException.class,
            () -> TextHelperGetTextBetweenAction.perform(mockedParameters, mockedParameters, null));
    }

    @Test
    void testExtractBetweenEmptyResult() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TEXT, "StartEnd", PATTERN_START, "Start", PATTERN_END, "End"));

        String result = TextHelperGetTextBetweenAction.perform(mockedParameters, mockedParameters, null);

        assertEquals("", result);
    }
}
