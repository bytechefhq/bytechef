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

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.monday.constant.MondayConstants.BOARDS;
import static com.bytechef.component.monday.constant.MondayConstants.BOARD_ID;
import static com.bytechef.component.monday.constant.MondayConstants.DATA;
import static com.bytechef.component.monday.constant.MondayConstants.ID;
import static com.bytechef.component.monday.constant.MondayConstants.NAME;
import static com.bytechef.component.monday.constant.MondayConstants.TITLE;
import static com.bytechef.component.monday.constant.MondayConstants.WORKSPACE_ID;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.monday.constant.MondayColumnType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Monika Kušter
 */
public class MondayOptionUtils {

    private MondayOptionUtils() {
    }

    public static List<Option<String>> getBoardIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        String query = "query{boards(workspace_ids: [%s], order_by: created_at){id name}}"
            .formatted(inputParameters.getRequiredString(WORKSPACE_ID));

        Map<String, Object> body = MondayUtils.executeGraphQLQuery(query, context);

        List<Option<String>> options = new ArrayList<>();

        if (body.get(DATA) instanceof Map<?, ?> map && map.get(BOARDS) instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> boardMap) {
                    options.add(option((String) boardMap.get(NAME), (String) boardMap.get(ID)));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getColumnTypeOptions() {
        return Arrays.stream(MondayColumnType.values())
            .map(mondayColumnType -> option(mondayColumnType.getDisplayValue(), mondayColumnType.getName()))
            .collect(Collectors.toList());
    }

    public static List<Option<String>> getGroupIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        String query =
            "query{boards(ids: [%s]){groups{id title}}}".formatted(inputParameters.getRequiredString(BOARD_ID));

        Map<String, Object> body = MondayUtils.executeGraphQLQuery(query, context);

        List<Option<String>> options = new ArrayList<>();

        if (body.get(DATA) instanceof Map<?, ?> map && map.get(BOARDS) instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> boardMap && boardMap.get("groups") instanceof List<?> groupList) {
                    for (Object group : groupList) {
                        if (group instanceof Map<?, ?> groupMap)
                            options.add(option((String) groupMap.get(TITLE), (String) groupMap.get(ID)));
                    }
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getWorkspaceIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        String query = "query{workspaces{id name}}";

        Map<String, Object> body = MondayUtils.executeGraphQLQuery(query, context);

        List<Option<String>> options = new ArrayList<>();

        if (body.get(DATA) instanceof Map<?, ?> map && map.get("workspaces") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> workspaceMap) {
                    options.add(option((String) workspaceMap.get(NAME), (String) workspaceMap.get(ID)));
                }
            }
        }

        return options;
    }
}
