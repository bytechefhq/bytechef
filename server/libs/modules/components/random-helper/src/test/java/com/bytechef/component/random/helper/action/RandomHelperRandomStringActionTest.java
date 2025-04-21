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

package com.bytechef.component.random.helper.action;

import static com.bytechef.component.random.helper.constant.RandomHelperConstants.ALPHANUMERIC_CHARACTERS;
import static com.bytechef.component.random.helper.constant.RandomHelperConstants.CHARACTER_SET;
import static com.bytechef.component.random.helper.constant.RandomHelperConstants.LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class RandomHelperRandomStringActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Parameters mockedParameters =
        MockParametersFactory.create(Map.of(LENGTH, 8, CHARACTER_SET, ALPHANUMERIC_CHARACTERS));

    @Test
    void testPerform() {
        String result = RandomHelperRandomStringAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(8, result.length());

        IntStream chars = result.chars();

        assertTrue(chars.allMatch(c -> ALPHANUMERIC_CHARACTERS.indexOf(c) >= 0));
    }
}
