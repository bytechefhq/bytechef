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

package com.bytechef.component.object.helper.action;

import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.SOURCE;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.SOURCE_TYPE;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.VALUE;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.object.helper.constant.ValueType;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.google.gson.Gson;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ObjectHelperAddKeyValuePairActionTest {

    private static final Gson gson = new Gson();

    @Test
    void testPerformForObject() {
        testWith("{'a':1}", "{'b':2}", "{'a':1,'b':2}");
        testWith("{'a':1}", "{'a':2}", "{'a':2}");
        testWith("{'a':1}", "{'a':2,'c':3}", "{'a':2,'c':3}");
        testWith("{'a':1}", "{'b':{'a':[{'c':1}]}}", "{'a':1,'b':{'a':[{'c':1}]}}");
    }

    @Test
    void testPerformForArray() {
        testWith("[{'a':1}]", "{'b':2}", "[{'a':1,'b':2}]");
        testWith("[{'a':1}, {'b':1}]", "{'a':2}", "[{'a':2},{'a':2,'b':1}]");
        testWith("[{'a':1}, {'b':1}]", "{'b':{'a':[{'c':1}]}}", "[{'a':1,'b':{'a':[{'c':1}]}},{'b':{'a':[{'c':1}]}}]");
    }

    private void testWith(String sourceJson, String valueJson, String expectedJson) {
        Object sourceObject = gson.fromJson(sourceJson, Object.class);
        Object valueObject = gson.fromJson(valueJson, Object.class);
        Object expectedObject = gson.fromJson(expectedJson, Object.class);
        ValueType sourceType = sourceJson.startsWith("[") ? ValueType.ARRAY : ValueType.OBJECT;

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(SOURCE, sourceObject, SOURCE_TYPE, sourceType.name(), VALUE, valueObject));

        Object resultObject = ObjectHelperAddKeyValuePairsAction.perform(
            mockedParameters, mockedParameters, mock(ActionContext.class));

        Assertions.assertEquals(expectedObject, resultObject);
    }
}
