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

import static com.bytechef.component.text.helper.action.TextHelperExtractContentFromHtmlAction.ReturnValue;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.ATTRIBUTE;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.CONTENT;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.QUERY_SELECTOR;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.RETURN_ARRAY;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.RETURN_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class TextHelperExtractContentFromHtmlActionTest {

    @Test
    void testPerformExtractText() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(
                CONTENT, "<div><p>Hello World</p><p>Goodbye World</p></div>", QUERY_SELECTOR, "p",
                RETURN_VALUE, ReturnValue.TEXT, RETURN_ARRAY, false));

        Object result = TextHelperExtractContentFromHtmlAction.perform(parameters, parameters, null);

        assertEquals("Hello World Goodbye World", result);
    }

    @Test
    void testPerformExtractHtml() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(
                CONTENT, "<div><p>Hello World</p><p>Goodbye World</p></div>", QUERY_SELECTOR, "p",
                RETURN_VALUE, ReturnValue.HTML, RETURN_ARRAY, true));

        Object result = TextHelperExtractContentFromHtmlAction.perform(parameters, parameters, null);

        assertEquals(List.of("Hello World", "Goodbye World"), result);
    }

    @Test
    void testPerformExtractAttribute() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(
                CONTENT, "<div><p class='greeting'>Hello World</p><p class='farewell'>Goodbye World</p></div>",
                QUERY_SELECTOR, "p", RETURN_VALUE, ReturnValue.ATTRIBUTE, ATTRIBUTE, "class", RETURN_ARRAY, true));

        Object result = TextHelperExtractContentFromHtmlAction.perform(parameters, parameters, null);

        assertEquals(List.of("greeting", "farewell"), result);
    }
}
