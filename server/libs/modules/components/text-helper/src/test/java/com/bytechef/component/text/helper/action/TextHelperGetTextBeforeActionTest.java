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

import static com.bytechef.component.text.helper.constant.TextHelperConstants.MATCH_NUMBER;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.PATTERN;
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
class TextHelperGetTextBeforeActionTest {

    @Test
    void testExtractBeforeMatchNormalCase() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                TEXT, "Hello world! This is a sample text. Hello again!",
                PATTERN, "Hello",
                MATCH_NUMBER, 2));

        String result = TextHelperGetTextBeforeAction.perform(mockedParameters, mockedParameters, null);

        assertEquals("Hello world! This is a sample text. ", result);
    }

    @Test
    void testExtractBeforeMatchFirstOccurrence() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TEXT, "Hello world! Hello again!", PATTERN, "Hello", MATCH_NUMBER, 1));

        String result = TextHelperGetTextBeforeAction.perform(mockedParameters, mockedParameters, null);

        assertEquals("", result);
    }

    @Test
    void testExtractBeforeMatchNotFound() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TEXT, "Hello world!", PATTERN, "Hi", MATCH_NUMBER, 1));

        assertThrows(ProviderException.class,
            () -> TextHelperGetTextBeforeAction.perform(mockedParameters, mockedParameters, null));
    }

    @Test
    void testExtractBeforeMatchMatchNumberTooHigh() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TEXT, "Hello world!", PATTERN, "Hello", MATCH_NUMBER, 2));

        assertThrows(ProviderException.class,
            () -> TextHelperGetTextBeforeAction.perform(mockedParameters, mockedParameters, null));
    }
}
