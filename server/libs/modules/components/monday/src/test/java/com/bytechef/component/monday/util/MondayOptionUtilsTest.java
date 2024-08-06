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
import static com.bytechef.component.monday.constant.MondayConstants.BOARD_ID;
import static com.bytechef.component.monday.constant.MondayConstants.DATA;
import static com.bytechef.component.monday.constant.MondayConstants.ID;
import static com.bytechef.component.monday.constant.MondayConstants.NAME;
import static com.bytechef.component.monday.constant.MondayConstants.TITLE;
import static com.bytechef.component.monday.constant.MondayConstants.WORKSPACE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class MondayOptionUtilsTest {

    private final List<Option<String>> expectedOptions = List.of(option("name", "123"));
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Context mockedContext = mock(Context.class);
    private MockedStatic<MondayUtils> mondayUtilsMockedStatic;

    @BeforeEach
    void beforeEach() {
        mondayUtilsMockedStatic = mockStatic(MondayUtils.class);
    }

    @AfterEach
    public void afterEach() {
        mondayUtilsMockedStatic.close();
    }

    @Test
    void testGetBoardIdOptions() {
        Map<String, Object> body = Map.of(DATA, Map.of("boards", List.of(Map.of(ID, "123", NAME, "name"))));

        when(mockedParameters.getRequiredString(WORKSPACE_ID))
            .thenReturn("abc");

        mondayUtilsMockedStatic.when(() -> MondayUtils.executeGraphQLQuery(any(Context.class), anyString()))
            .thenReturn(body);

        List<Option<String>> boardIdOptions =
            MondayOptionUtils.getBoardIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        mondayUtilsMockedStatic.verify(() -> MondayUtils.executeGraphQLQuery(mockedContext,
            "query{boards(workspace_ids: [abc], order_by: created_at){id name}}"));

        assertEquals(expectedOptions, boardIdOptions);
    }

    @Test
    void testGetGroupIdOptions() {
        Map<String, Object> body =
            Map.of(DATA, Map.of("boards", List.of(Map.of("groups", List.of(Map.of(ID, "123", TITLE, "name"))))));

        when(mockedParameters.getRequiredString(BOARD_ID))
            .thenReturn("abc");

        mondayUtilsMockedStatic.when(() -> MondayUtils.executeGraphQLQuery(any(Context.class), anyString()))
            .thenReturn(body);

        List<Option<String>> boardIdOptions =
            MondayOptionUtils.getGroupIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        mondayUtilsMockedStatic
            .verify(
                () -> MondayUtils.executeGraphQLQuery(mockedContext, "query{boards(ids: [abc]){groups{id title}}}"));

        assertEquals(expectedOptions, boardIdOptions);
    }

    @Test
    void testGetWorkspaceIdOptions() {
        Map<String, Object> body = Map.of(DATA, Map.of("workspaces", List.of(Map.of(ID, "123", NAME, "name"))));

        mondayUtilsMockedStatic.when(() -> MondayUtils.executeGraphQLQuery(any(Context.class), anyString()))
            .thenReturn(body);

        List<Option<String>> boardIdOptions =
            MondayOptionUtils.getWorkspaceIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        mondayUtilsMockedStatic
            .verify(() -> MondayUtils.executeGraphQLQuery(mockedContext, "query{workspaces{id name}}"));

        assertEquals(expectedOptions, boardIdOptions);
    }

}
