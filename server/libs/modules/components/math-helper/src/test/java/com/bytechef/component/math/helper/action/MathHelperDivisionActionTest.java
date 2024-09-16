/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.math.helper.action;

import static com.bytechef.component.math.helper.constants.MathHelperConstants.FIRST_NUMBER;
import static com.bytechef.component.math.helper.constants.MathHelperConstants.SECOND_NUMBER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class MathHelperDivisionActionTest {

    @Test
    void testPerform() {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(FIRST_NUMBER, 10, SECOND_NUMBER, 2));

        Double result = MathHelperDivisionAction.perform(mockedParameters, mockedParameters, mock(ActionContext.class));

        assertEquals(5.0, result);
    }

    @Test
    void testPerformDivisionByZero() {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(FIRST_NUMBER, 10, SECOND_NUMBER, 0));

        assertThrows(UnsupportedOperationException.class,
            () -> MathHelperDivisionAction.perform(mockedParameters, mockedParameters, mock(ActionContext.class)));
    }
}
