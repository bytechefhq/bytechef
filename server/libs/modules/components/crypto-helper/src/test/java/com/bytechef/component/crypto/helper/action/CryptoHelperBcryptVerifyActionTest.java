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

import static com.bytechef.component.helper.constant.CryptoHelperConstants.HASH;
import static com.bytechef.component.helper.constant.CryptoHelperConstants.PASSWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.helper.action.CryptoHelperBcryptVerifyAction;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Ivica Cardic
 */
class CryptoHelperBcryptVerifyActionTest {

    @ParameterizedTest
    @CsvSource(
        nullValues = "NULL",
        value = {
            "test, $2y$10$7UbRBioJHAzUwG2EDVfcMeF28cmRSCbCuDyNUPpmqJT5zI0Gz1oLO, true",
            "tesT, $2y$10$7UbRBioJHAzUwG2EDVfcMeF28cmRSCbCuDyNUPpmqJT5zI0Gz1oLO, false",
            "U*U, $2a$05$CCCCCCCCCCCCCCCCCCCCC.E5YPO9kmyuRGyh0XouQYb4YMJKvyOeW, true",
            "U*U, $2b$05$CCCCCCCCCCCCCCCCCCCCC.E5YPO9kmyuRGyh0XouQYb4YMJKvyOeW, true",
            "U*U*, $2a$05$CCCCCCCCCCCCCCCCCCCCC.VGOzA784oUp/Z0DY336zx7pLYAy0lwK, true",
            "'', $2a$05$CCCCCCCCCCCCCCCCCCCCC.7uG0VCzI2bS7j6ymqJi9CdcdxiRTWNy, true",
            "test, not a hash, false",
            "test, $2a$10$short, false",
            "test, '', false",
            "test, NULL, false",
            "NULL, $2y$10$7UbRBioJHAzUwG2EDVfcMeF28cmRSCbCuDyNUPpmqJT5zI0Gz1oLO, false"
        })
    void testPerform(String password, String hash, boolean expected) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put(PASSWORD, password);
        parameters.put(HASH, hash);

        Parameters mockedParameters = MockParametersFactory.create(parameters);

        assertEquals(
            expected, CryptoHelperBcryptVerifyAction.perform(mockedParameters, mockedParameters, mock(Context.class)));
    }
}
