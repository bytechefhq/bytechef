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

import static com.bytechef.component.text.helper.constant.TextHelperConstants.KEY_VALUE_OBJECT;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.REGULAR_EXPRESSION;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class TextHelperExtractKeyRegExActionTest {

    @Test
    void testPerform() {
        Map<String, Object> keyValueMap = Map.of("noDigits", "val1", "key99", "val2", "another100", "val3");
        String regex = "\\d+";

        Parameters mockedParameters =
            MockParametersFactory.create(Map.of(KEY_VALUE_OBJECT, keyValueMap, REGULAR_EXPRESSION, regex));

        List<String> result = TextHelperExtractKeyRegExAction.perform(mockedParameters, null, null);

        assertEquals(List.of("another100", "key99"), result);
    }
}
