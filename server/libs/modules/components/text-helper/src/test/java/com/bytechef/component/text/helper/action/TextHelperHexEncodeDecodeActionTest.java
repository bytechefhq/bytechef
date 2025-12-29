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
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.text.helper.constant.OperationType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class TextHelperHexEncodeDecodeActionTest {

    @Test
    void testPerform() {
        String text = "testing";

        Parameters encodeMockedParameters = MockParametersFactory.create(
            Map.of(TEXT, text, OPERATION, OperationType.ENCODE.name()));

        String encodedString = TextHelperHexEncodeDecodeAction.perform(
            encodeMockedParameters, null, null);

        Parameters decodeMockedParameters = MockParametersFactory.create(
            Map.of(TEXT, encodedString, OPERATION, OperationType.DECODE.name()));

        String decodedString = TextHelperHexEncodeDecodeAction.perform(
            decodeMockedParameters, null, null);

        assertEquals(text, decodedString);
    }
}
