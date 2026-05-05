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

package com.bytechef.component.linear.action;

import static com.bytechef.component.linear.constant.LinearConstants.DESCRIPTION;
import static com.bytechef.component.linear.constant.LinearConstants.NAME;
import static com.bytechef.component.linear.constant.LinearConstants.PRIORITY;
import static com.bytechef.component.linear.constant.LinearConstants.PROJECT_ID;
import static com.bytechef.component.linear.constant.LinearConstants.START_DATE;
import static com.bytechef.component.linear.constant.LinearConstants.STATUS_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.linear.util.LinearUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Marija Horvat
 */
@ExtendWith(MockContextSetupExtension.class)
class LinearUpdateProjectActionTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(ActionContext mockedContext) {
        Parameters parameters = MockParametersFactory.create(
            Map.of(PROJECT_ID, "1", NAME, "Name", STATUS_ID, "Done", PRIORITY, 0, START_DATE, "2025-05-15", DESCRIPTION,
                "This is a description."));

        try (MockedStatic<LinearUtils> linearUtilsMockedStatic = mockStatic(LinearUtils.class)) {
            linearUtilsMockedStatic
                .when(() -> LinearUtils.appendOptionalField(any(StringBuilder.class), anyString(), any(Object.class)))
                .thenCallRealMethod();
            linearUtilsMockedStatic
                .when(() -> LinearUtils.executeGraphQLQuery(
                    stringArgumentCaptor.capture(), contextArgumentCaptor.capture()))
                .thenReturn(Map.of("data", Map.of("projectUpdate", Map.of("id", "abc"))));

            Object result = LinearUpdateProjectAction.perform(parameters, null, mockedContext);

            assertEquals(Map.of("id", "abc"), result);
            assertEquals(
                "mutation{projectUpdate(input: { name: \"Name\", statusId: \"Done\", priority: 0, startDate: \"2025-05-15\", description: \"This is a description.\" } id: \"1\"){success project{id name}}}",
                stringArgumentCaptor.getValue());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
        }
    }
}
