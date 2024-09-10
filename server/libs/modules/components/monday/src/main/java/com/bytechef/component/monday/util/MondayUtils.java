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

import static com.bytechef.component.monday.constant.MondayConstants.BOARDS;
import static com.bytechef.component.monday.constant.MondayConstants.DATA;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MondayUtils {

    private MondayUtils() {
    }

    public static List<?> getBoardColumns(String boardId, ActionContext context) {
        String query = "query{boards(ids: %s){columns{id title type settings_str description}}}".formatted(boardId);

        Map<String, Object> result = executeGraphQLQuery(query, context);

        if (result.get(DATA) instanceof Map<?, ?> map &&
            map.get(BOARDS) instanceof List<?> list &&
            list.getFirst() instanceof Map<?, ?> boardMap &&
            boardMap.get("columns") instanceof List<?> columnList) {

            return columnList;
        }

        throw new ProviderException("Failed to get board columns");
    }

    public static Map<String, Object> executeGraphQLQuery(String query, Context context) {
        return context
            .http(http -> http.post(""))
            .body(Body.of(Map.of("query", query)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

}
