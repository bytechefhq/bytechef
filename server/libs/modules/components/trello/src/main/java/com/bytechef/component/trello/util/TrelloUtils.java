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

package com.bytechef.component.trello.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.trello.constant.TrelloConstants.ID;
import static com.bytechef.component.trello.constant.TrelloConstants.ID_BOARD;
import static com.bytechef.component.trello.constant.TrelloConstants.NAME;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class TrelloUtils {

    private TrelloUtils() {
    }

    public static List<Option<String>> getBoardOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        List<Map<String, Object>> body = context
            .http(http -> http.get("/members/" + getAuthorisedUserId(context) + "/boards"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body);
    }

    public static List<Option<String>> getCardOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        List<Map<String, Object>> body = context
            .http(http -> http.get("/boards/" + inputParameters.getRequiredString(ID_BOARD) + "/cards"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body);
    }

    public static List<Option<String>> getListOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        List<Map<String, Object>> body = context
            .http(http -> http.get("/boards/" + inputParameters.getRequiredString(ID_BOARD) + "/lists"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body);
    }

    private static String getAuthorisedUserId(Context context) {
        Map<String, Object> body = context
            .http(http -> http.get("/members/me"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return (String) body.get(ID);
    }

    private static List<Option<String>> getOptions(List<Map<String, Object>> body) {
        List<Option<String>> options = new ArrayList<>();

        for (Map<String, Object> item : body) {
            options.add(option((String) item.get(NAME), (String) item.get(ID)));
        }

        return options;
    }
}
