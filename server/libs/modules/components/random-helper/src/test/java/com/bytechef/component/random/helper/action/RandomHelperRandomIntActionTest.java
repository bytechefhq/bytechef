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

import static com.bytechef.component.random.helper.constant.RandomHelperConstants.END_INCLUSIVE;
import static com.bytechef.component.random.helper.constant.RandomHelperConstants.START_INCLUSIVE;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
class RandomHelperRandomIntActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Parameters mockedParameters =
        MockParametersFactory.create(Map.of(START_INCLUSIVE, 10, END_INCLUSIVE, 20));

    @Test
    void testPerform() {
        Integer result = RandomHelperRandomIntAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertNotNull(result);
        assertTrue(result >= 10 && result <= 20);
    }
}
