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

package com.bytechef.component.monday.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.monday.constant.MondayConstants.BOARDS;
import static com.bytechef.component.monday.constant.MondayConstants.BOARD_ID;
import static com.bytechef.component.monday.constant.MondayConstants.COLUMNS;
import static com.bytechef.component.monday.constant.MondayConstants.COLUMN_ID;
import static com.bytechef.component.monday.constant.MondayConstants.DATA;
import static com.bytechef.component.monday.constant.MondayConstants.ID;
import static com.bytechef.component.monday.constant.MondayConstants.LABELS;
import static com.bytechef.component.monday.constant.MondayConstants.NAME;
import static com.bytechef.component.monday.constant.MondayConstants.TITLE;
import static com.bytechef.component.monday.constant.MondayConstants.TYPE;
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
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Map<String, ?>> boards = getBoardsByWorkspace(inputParameters.getRequiredString(WORKSPACE_ID), context);

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, ?> board : boards) {
            options.add(option((String) board.get(NAME), (String) board.get(ID)));
        }

        return options;
    }

    public static List<Option<String>> getBoardIdWithStatusOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Map<String, ?>> boards = getBoardsByWorkspace(inputParameters.getRequiredString(WORKSPACE_ID), context);

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, ?> board : boards) {
            if (board.get(COLUMNS) instanceof List<?> columns) {
                boolean hasStatus = columns.stream()
                    .anyMatch(column -> {
                        if (column instanceof Map<?, ?> columnMap) {
                            String type = (String) columnMap.get(TYPE);

                            return type.equals("status");
                        }

                        return false;
                    });

                if (hasStatus) {
                    options.add(option((String) board.get(NAME), (String) board.get(ID)));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getBoardItemsOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Map<String, ?>> boards = getBoardsById(inputParameters.getRequiredString(BOARD_ID), context);

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, ?> board : boards) {
            if (board.get("items_page") instanceof Map<?, ?> itemsPage &&
                itemsPage.get("items") instanceof List<?> items) {
                for (Object item : items) {
                    if (item instanceof Map<?, ?> itemMap) {
                        options.add(option((String) itemMap.get(NAME), (String) itemMap.get(ID)));
                    }
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getColumnIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Map<String, ?>> boards = getBoardsById(inputParameters.getRequiredString(BOARD_ID), context);

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, ?> board : boards) {
            if (board.get(COLUMNS) instanceof List<?> columns) {
                for (Object column : columns) {
                    if (column instanceof Map<?, ?> columnMap) {
                        options.add(option((String) columnMap.get("title"), (String) columnMap.get(ID)));
                    }
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getColumnStatusIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Map<String, ?>> boards = getBoardsById(inputParameters.getRequiredString(BOARD_ID), context);

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, ?> board : boards) {
            if (board.get(COLUMNS) instanceof List<?> columns) {
                for (Object column : columns) {
                    if (column instanceof Map<?, ?> columnMap && columnMap.get(TYPE)
                        .equals("status")) {
                        options.add(option((String) columnMap.get("title"), (String) columnMap.get(ID)));
                    }
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
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Map<String, ?>> boards = getBoardsById(inputParameters.getRequiredString(BOARD_ID), context);

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, ?> board : boards) {
            if (board.get("groups") instanceof List<?> groupList) {
                for (Object group : groupList) {
                    if (group instanceof Map<?, ?> groupMap)
                        options.add(option((String) groupMap.get(TITLE), (String) groupMap.get(ID)));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getStatusOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Map<String, ?>> boards = getBoardsById(inputParameters.getRequiredString(BOARD_ID), context);

        List<Option<String>> options = new ArrayList<>();

        for (Map<String, ?> board : boards) {
            if (board.get(COLUMNS) instanceof List<?> columns) {
                for (Object column : columns) {
                    if (column instanceof Map<?, ?> columnMap &&
                        columnMap.get(ID)
                            .equals(inputParameters.getRequiredString(COLUMN_ID))
                        &&
                        columnMap.get("settings") instanceof Map<?, ?> settings &&
                        settings.get(LABELS) instanceof List<?> labels) {

                        for (Object label : labels) {
                            if (label instanceof Map<?, ?> labelMap) {
                                options.add(option((String) labelMap.get("label"), (String) labelMap.get("label")));
                            }
                        }
                    }
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getWorkspaceIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
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

    private static List<Map<String, ?>> getBoardsByWorkspace(String workspaceId, Context context) {
        String query =
            "query{boards(workspace_ids: [%s], order_by: created_at){id name columns {id title type settings}}}"
                .formatted(workspaceId);

        return extractBoards(MondayUtils.executeGraphQLQuery(query, context));
    }

    private static List<Map<String, ?>> getBoardsById(String boardId, Context context) {
        String query =
            "query{boards(ids: [%s]){id name columns {id title type settings} items_page{items{id name}} groups{id title}}}"
                .formatted(boardId);

        return extractBoards(MondayUtils.executeGraphQLQuery(query, context));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, ?>> extractBoards(Map<String, Object> body) {
        if (body.get(DATA) instanceof Map<?, ?> map &&
            map.get(BOARDS) instanceof List<?> list) {

            return (List<Map<String, ?>>) list;
        }
        return List.of();
    }
}
