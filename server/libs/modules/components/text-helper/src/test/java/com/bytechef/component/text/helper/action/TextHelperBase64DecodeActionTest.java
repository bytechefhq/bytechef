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

package com.bytechef.component.text.helper.action;

import static com.bytechef.component.text.helper.constant.TextHelperConstants.CONTENT;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.ENCODING_SCHEMA;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.ENCODING_SCHEMA_BASE64;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.ENCODING_SCHEMA_BASE64URL;
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
class TextHelperBase64DecodeActionTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);

    @Test
    void testPerformWithBase64Encoding() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(CONTENT, "SGVsbG8gd29ybGQ=", ENCODING_SCHEMA, ENCODING_SCHEMA_BASE64));

        Object result = TextHelperBase64DecodeAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals("Hello world", result);
    }

    @Test
    void testPerformWithBase64UrlEncoding() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(CONTENT, "SGVsbG8td29ybGQ", ENCODING_SCHEMA, ENCODING_SCHEMA_BASE64URL));

        Object result = TextHelperBase64DecodeAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals("Hello-world", result);
    }

    @Test
    void testPerformWithEmptyContent() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(CONTENT, "", ENCODING_SCHEMA, ENCODING_SCHEMA_BASE64));

        Object result = TextHelperBase64DecodeAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals("", result);
    }

    @Test
    void testPerformWithUnsupportedSchema() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(CONTENT, "SGVsbG8gd29ybGQ=", ENCODING_SCHEMA, "unsupported"));

        assertThrows(IllegalArgumentException.class,
            () -> TextHelperBase64DecodeAction.perform(mockedParameters, mockedParameters, mockedActionContext));
    }
}
