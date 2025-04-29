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

package com.bytechef.component.crypto.helper.action;

import static com.bytechef.component.helper.constant.CryptoHelperConstants.ALGORITHM;
import static com.bytechef.component.helper.constant.CryptoHelperConstants.INPUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.helper.action.CryptoHelperHashAction;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Nikolina Spehar
 */
class CryptoHelperHashActionTest {

    @ParameterizedTest
    @CsvSource({
        "MD5, 5eed650258ee02f6a77c87b748b764ec",
        "SHA1, 49883b34e5a0f48224dd6230f471e9dc1bdbeaf5",
        "SHA256, 9dfe6f15d1ab73af898739394fd22fd72a03db01834582f24bb2e1c66c7aaeae"
    })
    void testPerform(String algorithm, String expected) {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(ALGORITHM, algorithm, INPUT, "test input"));

        String result = CryptoHelperHashAction.perform(mockedParameters, mockedParameters, mock(Context.class));

        assertEquals(expected, result);
    }
}
