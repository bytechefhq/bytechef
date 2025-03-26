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

package com.bytechef.component.example.action;

import static com.bytechef.component.example.constant.CryptoHelperConstants.ALPHANUMERIC_CHARACTERS;
import static com.bytechef.component.example.constant.CryptoHelperConstants.CHARACTER_SET;
import static com.bytechef.component.example.constant.CryptoHelperConstants.LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class CryptoHelperGeneratePasswordActionTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Parameters mockedParameters =
        MockParametersFactory.create(Map.of(LENGTH, 8, CHARACTER_SET, ALPHANUMERIC_CHARACTERS));

    @Test
    void testPerform() {
        String password = CryptoHelperGeneratePasswordAction.perform(
            mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(8, password.length());
    }
}
