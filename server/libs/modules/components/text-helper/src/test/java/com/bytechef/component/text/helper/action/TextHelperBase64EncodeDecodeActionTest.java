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
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.text.helper.constant.OperationType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class TextHelperBase64EncodeDecodeActionTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);

    @Test
    void testPerformEncode() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TEXT, "SGVsbG8gd29ybGQ=", OPERATION, OperationType.DECODE.name()));

        Object result =
            TextHelperBase64EncodeDecodeAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals("Hello world", result);
    }

    @Test
    void testPerformDecode() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TEXT, "Hello world", OPERATION, OperationType.ENCODE.name()));

        Object result =
            TextHelperBase64EncodeDecodeAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals("SGVsbG8gd29ybGQ=", result);
    }

    @Test
    void testPerformWithEmptyContent() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(TEXT, "", OPERATION, OperationType.DECODE.name()));

        Object result =
            TextHelperBase64EncodeDecodeAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals("", result);
    }
}
