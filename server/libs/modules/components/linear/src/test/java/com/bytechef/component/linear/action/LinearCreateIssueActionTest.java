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

import static com.bytechef.component.linear.constant.LinearConstants.ASSIGNEE_ID;
import static com.bytechef.component.linear.constant.LinearConstants.DESCRIPTION;
import static com.bytechef.component.linear.constant.LinearConstants.PRIORITY;
import static com.bytechef.component.linear.constant.LinearConstants.STATUS_ID;
import static com.bytechef.component.linear.constant.LinearConstants.TEAM_ID;
import static com.bytechef.component.linear.constant.LinearConstants.TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.linear.util.LinearUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Marija Horvat
 */
class LinearCreateIssueActionTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(TITLE, "Title", TEAM_ID, "1", STATUS_ID, "Done", PRIORITY, 0, ASSIGNEE_ID, "abc", DESCRIPTION,
                "This is a description."));

        try (MockedStatic<LinearUtils> linearUtilsMockedStatic = mockStatic(LinearUtils.class)) {
            linearUtilsMockedStatic
                .when(() -> LinearUtils.appendOptionalField(any(StringBuilder.class), anyString(), any(Object.class)))
                .thenCallRealMethod();
            linearUtilsMockedStatic
                .when(() -> LinearUtils.executeGraphQLQuery(
                    stringArgumentCaptor.capture(), contextArgumentCaptor.capture()))
                .thenReturn(Map.of("data", Map.of("issueCreate", Map.of("id", "abc"))));

            Object result = LinearCreateIssueAction.perform(parameters, parameters, mockedActionContext);

            assertEquals(Map.of("id", "abc"), result);
            assertEquals(
                "mutation{issueCreate(input: {title: \"Title\", teamId: \"1\", stateId: \"Done\", priority: 0, assigneeId: \"abc\", description: \"This is a description.\"}){success issue{id title}}}",
                stringArgumentCaptor.getValue());
            assertEquals(mockedActionContext, contextArgumentCaptor.getValue());
        }
    }
}
