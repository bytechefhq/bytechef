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

import static com.bytechef.component.text.helper.constant.TextHelperConstants.REPLACE_ONLY_FIRST;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.REPLACE_VALUE;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.SEARCH_VALUE;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class TextHelperReplaceActionTest {

    @Test
    void testPerform() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(TEXT, "apple banana apple", SEARCH_VALUE, "apple", REPLACE_VALUE, "orange", REPLACE_ONLY_FIRST,
                false));

        String result = TextHelperReplaceAction.perform(parameters, parameters, null);

        String expected = "orange banana orange";

        assertEquals(expected, result);
    }

    @Test
    void testPerformReplaceOnlyFirst() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(TEXT, "apple banana apple", SEARCH_VALUE, "apple", REPLACE_VALUE, "orange", REPLACE_ONLY_FIRST,
                true));

        String result = TextHelperReplaceAction.perform(parameters, parameters, null);

        String expected = "orange banana apple";

        assertEquals(expected, result);
    }
}
