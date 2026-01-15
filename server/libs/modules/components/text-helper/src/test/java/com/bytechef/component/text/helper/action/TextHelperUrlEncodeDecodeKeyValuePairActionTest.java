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

import static com.bytechef.component.text.helper.constant.TextHelperConstants.OPERATION;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.PAIRS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.text.helper.constant.OperationType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class TextHelperUrlEncodeDecodeKeyValuePairActionTest {

    @Test
    void testPerform() {
        Map<String, String> pairs = Map.of("key1", "value1", "key2", "value2");

        Parameters mockParametersEncode = MockParametersFactory.create(Map.of(
            PAIRS, pairs, OPERATION, OperationType.ENCODE));

        Map<String, String> encodedPairs = TextHelperUrlEncodeDecodeKeyValuePairAction.perform(
            mockParametersEncode, null, null);

        Parameters mockParametersDecode = MockParametersFactory.create(Map.of(
            PAIRS, encodedPairs, OPERATION, OperationType.DECODE));

        Map<String, String> decodedPairs = TextHelperUrlEncodeDecodeKeyValuePairAction.perform(
            mockParametersDecode, null, null);

        assertEquals(pairs, decodedPairs);
    }
}
