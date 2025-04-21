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

package com.bytechef.component.acumbamail.action;

import static com.bytechef.component.acumbamail.constant.AcumbamailConstants.LIST_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class AcumbamailAddSubscriberActionTest extends AbstractAcumbamailActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(LIST_ID, "test"));

    @Test
    void testPerform() {
        Object result = AcumbamailAddSubscriberAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(responseMap, result);
        assertEquals(List.of(LIST_ID, "test"), stringArgumentCaptor.getAllValues());
    }
}
