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

import static com.bytechef.component.text.helper.constant.TextHelperConstants.DISPLAY_NAME;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.DOMAIN;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.EMAIL;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.LOCAL_PART;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class TextHelperParseEmailActionTest {

    @Test
    void testPerformWithDisplayName() {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(EMAIL, "John Doe <john.doe@example.com>"));

        Map<String, String> result = TextHelperParseEmailAction.perform(mockedParameters, null, null);

        assertEquals("John Doe", result.get(DISPLAY_NAME));
        assertEquals("john.doe", result.get(LOCAL_PART));
        assertEquals("example.com", result.get(DOMAIN));
        assertEquals("john.doe@example.com", result.get(EMAIL));
    }

    @Test
    void testPerformWithoutDisplayName() {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(EMAIL, "jane.doe@example.com"));

        Map<String, String> result = TextHelperParseEmailAction.perform(mockedParameters, null, null);

        assertEquals("", result.get(DISPLAY_NAME));
        assertEquals("jane.doe", result.get(LOCAL_PART));
        assertEquals("example.com", result.get(DOMAIN));
        assertEquals("jane.doe@example.com", result.get(EMAIL));
    }
}
