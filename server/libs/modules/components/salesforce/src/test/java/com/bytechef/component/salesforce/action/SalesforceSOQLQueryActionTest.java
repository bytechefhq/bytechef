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

package com.bytechef.component.salesforce.action;

import static com.bytechef.component.salesforce.constant.SalesforceConstants.Q;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.salesforce.util.SalesforceUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class SalesforceSOQLQueryActionTest {

    private final ArgumentCaptor<ActionContext> actionContextArgumentCaptor =
        ArgumentCaptor.forClass(ActionContext.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(Q, "query"));
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() {
        try (MockedStatic<SalesforceUtils> salesforceUtilsMockedStatic = mockStatic(SalesforceUtils.class)) {
            salesforceUtilsMockedStatic
                .when(() -> SalesforceUtils.executeSOQLQuery(
                    actionContextArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(Map.of());

            Object result = SalesforceSOQLQueryAction.perform(mockedParameters, mockedParameters, mockedActionContext);

            assertEquals(Map.of(), result);
            assertEquals("query", stringArgumentCaptor.getValue());
            assertEquals(mockedActionContext, actionContextArgumentCaptor.getValue());
        }
    }
}
