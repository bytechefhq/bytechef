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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Nikolina Spehar
 */
class TextHelperStripHtmlTagsActionTest {

    @ParameterizedTest
    @CsvSource(value = {
        "'<p>Hello <b>World</b></p>', 'Hello World'",
        "'Visit <a href=''https://example.com''>Example</a>!', 'Visit Example!'",
        "'', ''",
        "'<div><span></span></div>', ''",
        "'<div>Hello <span>World <b>Bold</b></span></div>', 'Hello World Bold'",
        "'<p>Hello <b>World</p>', 'Hello World'"
    })
    void testPerform(String input, String expectedResult) {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(TEXT, input == null ? "" : input));

        assertEquals(expectedResult, TextHelperStripHtmlTagsAction.perform(mockedParameters, null, null));
    }
}
