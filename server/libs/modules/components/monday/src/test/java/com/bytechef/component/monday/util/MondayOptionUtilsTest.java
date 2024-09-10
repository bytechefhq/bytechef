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

package com.bytechef.component.monday.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.monday.constant.MondayConstants.BOARDS;
import static com.bytechef.component.monday.constant.MondayConstants.BOARD_ID;
import static com.bytechef.component.monday.constant.MondayConstants.DATA;
import static com.bytechef.component.monday.constant.MondayConstants.ID;
import static com.bytechef.component.monday.constant.MondayConstants.NAME;
import static com.bytechef.component.monday.constant.MondayConstants.TITLE;
import static com.bytechef.component.monday.constant.MondayConstants.WORKSPACE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.monday.constant.MondayColumnType;
import com.bytechef.test.component.properties.ParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class MondayOptionUtilsTest {

    private final List<Option<String>> expectedOptions = List.of(option("name", "123"));
    private final Context mockedContext = mock(Context.class);
    private final Parameters parameters = ParametersFactory.createParameters(
        Map.of(WORKSPACE_ID, "abc", BOARD_ID, "abc"));

    @Test
    void testGetBoardIdOptions() {
        Map<String, Object> body = Map.of(DATA, Map.of(BOARDS, List.of(Map.of(ID, "123", NAME, "name"))));

        try (MockedStatic<MondayUtils> mondayUtilsMockedStatic = mockStatic(MondayUtils.class)) {
            mondayUtilsMockedStatic
                .when(() -> MondayUtils.executeGraphQLQuery(anyString(), any(Context.class)))
                .thenReturn(body);

            List<Option<String>> boardIdOptions =
                MondayOptionUtils.getBoardIdOptions(parameters, parameters, Map.of(), "", mockedContext);

            mondayUtilsMockedStatic.verify(() -> MondayUtils.executeGraphQLQuery(
                "query{boards(workspace_ids: [abc], order_by: created_at){id name}}", mockedContext));

            assertEquals(expectedOptions, boardIdOptions);
        }
    }

    @Test
    void testGetColumnTypeOptions() {
        List<Option<String>> options = MondayOptionUtils.getColumnTypeOptions();

        assertNotNull(options);
        assertFalse(options.isEmpty());

        for (MondayColumnType type : MondayColumnType.values()) {
            Option<String> expectedOption = option(type.getDisplayValue(), type.getName());

            assertTrue(options.contains(expectedOption), "Expected option not found: " + expectedOption);
        }
    }

    @Test
    void testGetGroupIdOptions() {
        Map<String, Object> body = Map.of(
            DATA, Map.of(BOARDS, List.of(Map.of("groups", List.of(Map.of(ID, "123", TITLE, "name"))))));

        try (MockedStatic<MondayUtils> mondayUtilsMockedStatic = mockStatic(MondayUtils.class)) {
            mondayUtilsMockedStatic
                .when(() -> MondayUtils.executeGraphQLQuery(anyString(), any(Context.class)))
                .thenReturn(body);

            List<Option<String>> groupIdOptions =
                MondayOptionUtils.getGroupIdOptions(parameters, parameters, Map.of(), "", mockedContext);

            mondayUtilsMockedStatic.verify(
                () -> MondayUtils.executeGraphQLQuery("query{boards(ids: [abc]){groups{id title}}}", mockedContext));

            assertEquals(expectedOptions, groupIdOptions);
        }
    }

    @Test
    void testGetWorkspaceIdOptions() {
        Map<String, Object> body = Map.of(DATA, Map.of("workspaces", List.of(Map.of(ID, "123", NAME, "name"))));

        try (MockedStatic<MondayUtils> mondayUtilsMockedStatic = mockStatic(MondayUtils.class)) {
            mondayUtilsMockedStatic
                .when(() -> MondayUtils.executeGraphQLQuery(anyString(), any(Context.class)))
                .thenReturn(body);

            List<Option<String>> workspaceIdOptions = MondayOptionUtils.getWorkspaceIdOptions(
                parameters, parameters, Map.of(), "", mockedContext);

            mondayUtilsMockedStatic.verify(
                () -> MondayUtils.executeGraphQLQuery("query{workspaces{id name}}", mockedContext));

            assertEquals(expectedOptions, workspaceIdOptions);
        }
    }
}
