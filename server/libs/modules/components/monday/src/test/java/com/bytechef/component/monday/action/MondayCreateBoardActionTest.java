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

package com.bytechef.component.monday.action;

import static com.bytechef.component.monday.constant.MondayConstants.BOARD_KIND;
import static com.bytechef.component.monday.constant.MondayConstants.BOARD_NAME;
import static com.bytechef.component.monday.constant.MondayConstants.DESCRIPTION;
import static com.bytechef.component.monday.constant.MondayConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.monday.util.MondayUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Kalaiyarasan Raja
 */
class MondayCreateBoardActionTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<Map<String, Object>> variablesArgumentCaptor = forClass(Map.class);

    @Test
    void testPerform() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(BOARD_NAME, "Test Board", DESCRIPTION, "Sample Description", BOARD_KIND, "public"));

        try (MockedStatic<MondayUtils> mondayUtilsMockedStatic = mockStatic(MondayUtils.class)) {
            mondayUtilsMockedStatic
                .when(() -> MondayUtils.executeGraphQLQuery(
                    stringArgumentCaptor.capture(), variablesArgumentCaptor.capture(),
                    contextArgumentCaptor.capture()))
                .thenReturn(Map.of("data", Map.of(ID, "abc")));

            Object result = MondayCreateBoardAction.perform(parameters, parameters, mockedActionContext);

            assertEquals(Map.of(ID, "abc"), result);
            assertEquals(
                "mutation($boardName: String!, $description: String, $boardKind: BoardKind!) {" +
                    "create_board(board_name: $boardName, description: $description, board_kind: $boardKind){id name}}",
                stringArgumentCaptor.getValue());
            assertEquals(
                Map.of("boardName", "Test Board", "description", "Sample Description", "boardKind", "public"),
                variablesArgumentCaptor.getValue());
            assertEquals(mockedActionContext, contextArgumentCaptor.getValue());
        }
    }
}
