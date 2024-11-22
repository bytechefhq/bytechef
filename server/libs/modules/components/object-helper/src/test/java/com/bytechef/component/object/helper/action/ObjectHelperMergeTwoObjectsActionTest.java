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
import static com.bytechef.component.object.helper.constant.ObjectHelperConstants.TARGET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class ObjectHelperMergeTwoObjectsActionTest {

    private final ActionContext actionContext = mock(ActionContext.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            SOURCE, Map.of("key1", "value1", "key2", "value2"),
            TARGET, Map.of("key1", "newValue", "key2", "value2")));

    @Test
    void testPerform() {
        Object result = ObjectHelperMergeTwoObjectsAction.perform(mockedParameters, mockedParameters, actionContext);

        assertEquals(Map.of("key1", "value1", "key2", "value2"), result);
    }
}
