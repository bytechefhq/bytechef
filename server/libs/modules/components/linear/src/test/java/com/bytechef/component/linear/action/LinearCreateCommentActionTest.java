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

import static com.bytechef.component.linear.constant.LinearConstants.BODY;
import static com.bytechef.component.linear.constant.LinearConstants.ISSUE_ID;
import static com.bytechef.component.linear.constant.LinearConstants.TEAM_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
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
class LinearCreateCommentActionTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(ActionContext mockedContext) {
        Parameters parameters = MockParametersFactory.create(
            Map.of(TEAM_ID, "1", ISSUE_ID, "2", BODY, "This is a comment."));

        try (MockedStatic<LinearUtils> linearUtilsMockedStatic = mockStatic(LinearUtils.class)) {
            linearUtilsMockedStatic
                .when(() -> LinearUtils.executeGraphQLQuery(
                    stringArgumentCaptor.capture(), contextArgumentCaptor.capture()))
                .thenReturn(Map.of("data", Map.of("commentCreate", Map.of("id", "abc"))));

            Object result = LinearCreateCommentAction.perform(parameters, null, mockedContext);

            assertEquals(Map.of("id", "abc"), result);
            assertEquals(
                "mutation{commentCreate(input: {issueId: \"2\", body: \"This is a comment.\"}){success comment{id issue{id} body}}}",
                stringArgumentCaptor.getValue());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
        }
    }
}
