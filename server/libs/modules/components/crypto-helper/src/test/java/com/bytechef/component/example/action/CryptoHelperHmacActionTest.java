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
import static com.bytechef.component.example.constant.CryptoHelperConstants.KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Nikolina Spehar
 */
class CryptoHelperHmacActionTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);

    @ParameterizedTest
    @CsvSource({
        "HmacMD5, 626d45975138d2543278261a81575f36",
        "HmacSHA1, 9912b5f2b0b4356049f5ca692500f1ee408c17a8",
        "HmacSHA256, d6f01175dbe45290787c9b2d2832fb6b05765184643931a8898848a284958822"
    })
    void testPerform(String algorithm, String expected) {
        Parameters mockedParameters =
            MockParametersFactory.create(Map.of(ALGORITHM, algorithm, INPUT, "test input", KEY, "test key"));

        String result = CryptoHelperHmacAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(expected, result);
    }
}
