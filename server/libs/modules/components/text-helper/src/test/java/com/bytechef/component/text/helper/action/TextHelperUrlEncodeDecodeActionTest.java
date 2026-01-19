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
 * @author Monika Kušter
 */
class TextHelperUrlEncodeDecodeActionTest {

    @Test
    void testPerformEncode() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TEXT, "Hello+World%21", OPERATION, OperationType.DECODE.name()));

        Object result = TextHelperUrlEncodeDecodeAction.perform(mockedParameters, null, null);

        assertEquals("Hello World!", result);
    }

    @Test
    void testPerformDecode() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TEXT, "Hello World!", OPERATION, OperationType.ENCODE.name()));

        Object result = TextHelperUrlEncodeDecodeAction.perform(mockedParameters, null, null);

        assertEquals("Hello+World%21", result);
    }

    @Test
    void testPerformWithEmptyContent() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TEXT, "", OPERATION, OperationType.DECODE.name()));

        Object result = TextHelperUrlEncodeDecodeAction.perform(mockedParameters, null, null);

        assertEquals("", result);
    }
}
