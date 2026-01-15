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

import static com.bytechef.component.text.helper.constant.TextHelperConstants.URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.AssertionsKt.assertNull;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class TextHelperParseUrlActionTest {

    @Test
    void testPerform() {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(
            URL, "https://www.example.com:8080/path/to/page?param1=value1&param2=value2#section1"));

        Map<String, Object> result = TextHelperParseUrlAction.perform(
            mockedParameters, null, null);

        assertEquals("https:", result.get("protocol"));
        assertEquals(true, result.get("slashes"));
        assertNull(result.get("auth"));
        assertEquals("www.example.com:8080", result.get("host"));
        assertEquals("8080", result.get("port"));
        assertEquals("www.example.com", result.get("hostname"));
        assertEquals("#section1", result.get("hash"));
        assertEquals("?param1=value1&param2=value2", result.get("search"));
        assertEquals("/path/to/page", result.get("pathname"));
        assertEquals("/path/to/page?param1=value1&param2=value2", result.get("path"));
        assertEquals(
            "https://www.example.com:8080/path/to/page?param1=value1&param2=value2#section1",
            result.get("href"));

        @SuppressWarnings("unchecked")
        Map<String, String> queryMap = (Map<String, String>) result.get("query");
        assertEquals(2, queryMap.size());
        assertEquals("value1", queryMap.get("param1"));
        assertEquals("value2", queryMap.get("param2"));
    }
}
