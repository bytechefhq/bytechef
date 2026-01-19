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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class TextHelperChangeTypeActionTest {

    @Test
    void testPerformValidNumber() {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(TEXT, "42"));
        Context mockedContext = mock(Context.class);

        double result = TextHelperChangeTypeAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals(42.0, result);
    }

    @Test
    void testPerformInvalidNumber() {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(TEXT, "not a number"));
        Context mockedContext = mock(Context.class);

        assertThrows(NumberFormatException.class,
            () -> TextHelperChangeTypeAction.perform(mockedParameters, mockedParameters, mockedContext));
    }
}
