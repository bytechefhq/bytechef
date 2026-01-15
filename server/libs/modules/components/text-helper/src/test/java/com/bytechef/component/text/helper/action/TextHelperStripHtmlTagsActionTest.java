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
class TextHelperStripHtmlTagsActionTest {

    private static String run(String input) {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(TEXT, input));

        return TextHelperStripHtmlTagsAction.perform(mockedParameters, null, null);
    }

    @Test
    void testSimpleHtml() {
        String html = "<p>Hello <b>World</b></p>";
        String expected = "Hello World";

        assertEquals(expected, run(html));
    }

    @Test
    void testHtmlWithLinks() {
        String html = "Visit <a href='https://example.com'>Example</a>!";
        String expected = "Visit Example!";

        assertEquals(expected, run(html));
    }

    @Test
    void testEmptyString() {
        String html = "";
        String expected = "";

        assertEquals(expected, run(html));
    }

    @Test
    void testOnlyTags() {
        String html = "<div><span></span></div>";
        String expected = "";

        assertEquals(expected, run(html));
    }

    @Test
    void testNestedTags() {
        String html = "<div>Hello <span>World <b>Bold</b></span></div>";
        String expected = "Hello World Bold";

        assertEquals(expected, run(html));
    }

    @Test
    void testMalformedHtml() {
        String html = "<p>Hello <b>World</p>";
        String expected = "Hello World";

        assertEquals(expected, run(html));
    }
}
