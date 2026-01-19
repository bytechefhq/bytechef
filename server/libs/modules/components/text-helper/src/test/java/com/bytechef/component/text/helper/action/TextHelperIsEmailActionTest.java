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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class TextHelperIsEmailActionTest {

    @Test
    void testPerformValidEmail() {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(TEXT, "alice@example.com"));

        boolean result = TextHelperIsEmailAction.perform(mockedParameters, null, null);

        assertTrue(result);
    }

    @Test
    void testPerformInvalidEmail() {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(TEXT, "invalid-email"));

        boolean result = TextHelperIsEmailAction.perform(mockedParameters, null, null);

        assertFalse(result);
    }
}
