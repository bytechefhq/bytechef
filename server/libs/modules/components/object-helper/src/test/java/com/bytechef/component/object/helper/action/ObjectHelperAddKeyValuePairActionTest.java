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

package com.bytechef.component.object.helper.action;

import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.LIST;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.SOURCE;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ObjectHelperAddKeyValuePairActionTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testPerform() {
        testWith("{\"a\":1}", "[{\"key\":\"b\", \"value\":2}]", "{\"a\":1,\"b\":2}");
        testWith("{\"a\":1}", "[{\"key\":\"a\", \"value\":2}]", "{\"a\":2}");
        testWith("{\"a\":1}", "[{\"key\":\"a\", \"value\":2}, {\"key\":\"c\", \"value\":3}]", "{\"a\":2,\"c\":3}");
        testWith("{\"a\":1}", "[{\"key\":\"b\", \"value\":{\"a\":[{\"c\":1}]}}]",
            "{\"a\":1,\"b\":{\"a\":[{\"c\":1}]}}");
    }

    private void testWith(String sourceJson, String valueJson, String expectedJson) {
        Object sourceObject = objectMapper.readValue(sourceJson, Object.class);
        Object valueObject = objectMapper.readValue(valueJson, Object.class);
        Object expectedObject = objectMapper.readValue(expectedJson, Object.class);

        Parameters mockedParameters = MockParametersFactory.create(Map.of(SOURCE, sourceObject, LIST, valueObject));

        Object resultObject = ObjectHelperAddKeyValuePairsAction.perform(
            mockedParameters, mockedParameters, mock(ActionContext.class));

        Assertions.assertEquals(expectedObject, resultObject);
    }
}
