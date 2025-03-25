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

import static com.bytechef.component.example.constant.CryptoHelperConstants.ALGORITHM;
import static com.bytechef.component.example.constant.CryptoHelperConstants.INPUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class CryptoHelperHashActionTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Parameters mockedParameters = mock(Parameters.class);

    @Test
    void testPerformMD5() {
        String result = mockPerform("MD5");
        String expected = "5eed650258ee02f6a77c87b748b764ec";

        assertEquals(expected, result);
    }

    @Test
    void testPerformSHA1() {
        String result = mockPerform("SHA1");
        String expected = "49883b34e5a0f48224dd6230f471e9dc1bdbeaf5";

        assertEquals(expected, result);
    }

    @Test
    void testPerformSHA256() {
        String result = mockPerform("SHA256");
        String expected = "9dfe6f15d1ab73af898739394fd22fd72a03db01834582f24bb2e1c66c7aaeae";

        assertEquals(expected, result);
    }

    private String mockPerform(String algorithm) {
        when(mockedParameters.getRequiredString(ALGORITHM))
            .thenReturn(algorithm);
        when(mockedParameters.getRequiredString(INPUT))
            .thenReturn("test input");

        return CryptoHelperHashAction.perform(mockedParameters, mockedParameters, mockedActionContext);
    }
}
