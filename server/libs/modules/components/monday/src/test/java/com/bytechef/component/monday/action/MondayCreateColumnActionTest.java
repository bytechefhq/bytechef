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

package com.bytechef.component.monday.action;

import static com.bytechef.component.monday.constant.MondayConstants.BOARD_ID;
import static com.bytechef.component.monday.constant.MondayConstants.COLUMN_TYPE;
import static com.bytechef.component.monday.constant.MondayConstants.ID;
import static com.bytechef.component.monday.constant.MondayConstants.TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * @author Monika Kušter
 */
class MondayCreateColumnActionTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(BOARD_ID, "board", TITLE, "title", COLUMN_TYPE, "date"));

        try (MockedStatic<MondayUtils> mondayUtilsMockedStatic = mockStatic(MondayUtils.class)) {
            mondayUtilsMockedStatic
                .when(() -> MondayUtils.executeGraphQLQuery(
                    stringArgumentCaptor.capture(), contextArgumentCaptor.capture()))
                .thenReturn(Map.of("data", Map.of(ID, "abc")));

            Object result = MondayCreateColumnAction.perform(parameters, parameters, mockedActionContext);

            assertEquals(Map.of(ID, "abc"), result);
            assertEquals(
                "mutation{create_column(board_id: board, title: \"title\", column_type: date){id title}}",
                stringArgumentCaptor.getValue());
            assertEquals(mockedActionContext, contextArgumentCaptor.getValue());
        }
    }
}
