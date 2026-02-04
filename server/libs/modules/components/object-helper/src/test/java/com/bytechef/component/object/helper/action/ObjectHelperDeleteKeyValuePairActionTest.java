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

import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.KEY;
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Kristián Stutiak
 */
class ObjectHelperDeleteKeyValuePairActionTest {

    private final ActionContext actionContext = Mockito.mock(ActionContext.class);
    private Parameters mockedParameters;

    @Test
    void testPerformDeleteKeyValuePair() {
        mockedParameters = MockParametersFactory.create(
            Map.of(SOURCE, Map.of("key1", "value1", "key2", "value2"), KEY, "key1"));

        Object result = ObjectHelperDeleteKeyValuePairAction.perform(mockedParameters, mockedParameters, actionContext);

        assertEquals(result, Map.of("key2", "value2"));
    }

    @Test
    void testPerformDeleteNonExistentKey() {
        mockedParameters = MockParametersFactory.create(
            Map.of(SOURCE, Map.of("key1", "value1"), KEY, "nonExistentKey"));

        Object result = ObjectHelperDeleteKeyValuePairAction.perform(mockedParameters, mockedParameters, actionContext);

        assertEquals(result, Map.of("key1", "value1"));
    }
}
