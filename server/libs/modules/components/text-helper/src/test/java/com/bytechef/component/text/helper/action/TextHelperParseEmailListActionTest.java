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
import static com.bytechef.component.text.helper.constant.TextHelperConstants.EMAILS;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.LOCAL_PART;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class TextHelperParseEmailListActionTest {

    @Test
    void testPerform() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(EMAILS, List.of(
                "John Doe <john.doe@example.com>", "Jane Doe <jane.doe@example.com>", "jack.smith@example.com")));

        List<Map<String, String>> result = TextHelperParseEmailListAction.perform(mockedParameters, null, null);

        List<Map<String, String>> expected = List.of(
            Map.of(
                DISPLAY_NAME, "John Doe",
                LOCAL_PART, "john.doe",
                DOMAIN, "example.com",
                EMAIL, "john.doe@example.com"),
            Map.of(
                DISPLAY_NAME, "Jane Doe",
                LOCAL_PART, "jane.doe",
                DOMAIN, "example.com",
                EMAIL, "jane.doe@example.com"),
            Map.of(
                DISPLAY_NAME, "",
                LOCAL_PART, "jack.smith",
                DOMAIN, "example.com",
                EMAIL, "jack.smith@example.com"));

        assertEquals(expected, result);
    }
}
