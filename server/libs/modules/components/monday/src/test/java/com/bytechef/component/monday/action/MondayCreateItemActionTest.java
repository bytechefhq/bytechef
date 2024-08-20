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
import static com.bytechef.component.monday.constant.MondayConstants.GROUP_ID;
import static com.bytechef.component.monday.constant.MondayConstants.ID;
import static com.bytechef.component.monday.constant.MondayConstants.ITEM_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.monday.util.MondayPropertiesUtils;
import com.bytechef.component.monday.util.MondayUtils;
import com.bytechef.test.component.properties.ParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class MondayCreateItemActionTest extends AbstractMondayActionTest {

    @Test
    void testPerform() {
        Map<String, Object> columnValueMap = Map.of(
            "date_id", "2024-08-21",
            "status_id", "Stuck",
            "world_clock_id", "Europe/Zagreb",
            "numbers_id", 3454,
            "email_id", "test@mail.com",
            "checkbox_id", true,
            "dropdown_id", List.of("a"),
            "link_id", "link",
            "text_id", "abc 123",
            "rating_id", 3);

        Parameters parameters = ParametersFactory.createParameters(
            Map.of(
                BOARD_ID, "board",
                GROUP_ID, "group",
                ITEM_NAME, "name",
                "columnValues", columnValueMap));

        try (MockedStatic<MondayPropertiesUtils> mondayUtilsMockedStatic = mockStatic(MondayPropertiesUtils.class)) {
            mondayUtilsMockedStatic.when(
                () -> MondayPropertiesUtils.convertPropertyToMondayColumnValue(columnValueMap, "board",
                    mockedActionContext))
                .thenReturn(Map.of());

            String jsonString = "{\"date_id\":{\"date\":\"2024-08-21\"},\"status_id\":{\"label\":\"Stuck\"}," +
                "\"world_clock_id\":{\"timezone\":\"Europe/Zagreb\"},\"numbers_id\":\"3454.6\"," +
                "\"email_id\":{\"email\":\"test@mail.com\",\"text\":\"test@maigfgl.com\"}," +
                "\"checkbox_id\":{\"checked\":true},\"dropdown_id\":{\"labels\":[\"a\"]}," +
                "\"link_id\":{\"url\":\"link\",\"text\":\"link\"},\"text_id\":\"abc 123\",\"rating_id\":{\"rating\":3}";

            when(mockedActionContext.json(any())).thenReturn(jsonString);
            Object result = MondayCreateItemAction.perform(parameters, parameters, mockedActionContext);

            assertEquals(Map.of(ID, "abc"), result);

            mondayUtilsMockedStatic.verify(() -> MondayUtils.executeGraphQLQuery(mockedActionContext,
                "mutation{create_item(board_id: board, group_id: \"group\", item_name: \"name\", column_values:\"%s\"){id name}}"
                    .formatted(jsonString.replace("\"", "\\\""))));
        }
    }
}
