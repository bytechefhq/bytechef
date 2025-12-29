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

import static com.bytechef.component.text.helper.constant.TextHelperConstants.EXPRESSION;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class TextHelperContainsActionTest {

    @Test
    void testPerformTextContainsExpression() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(TEXT, "This is a sample text.", EXPRESSION, "sample"));

        assertTrue(TextHelperContainsAction.perform(parameters, parameters, mock(Context.class)));
    }

    @Test
    void testPerformTextDoesNotContainExpression() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(TEXT, "This is a sample text.", EXPRESSION, "missing"));

        assertFalse(TextHelperContainsAction.perform(parameters, parameters, mock(Context.class)));
    }
}
