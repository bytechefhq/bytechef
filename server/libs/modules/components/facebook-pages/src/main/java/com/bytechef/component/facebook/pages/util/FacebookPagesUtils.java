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

package com.bytechef.component.facebook.pages.util;

import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.BEARER;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.ACCESS_TOKEN;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.DATA;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.ID;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.MESSAGE;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.NAME;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class FacebookPagesUtils {

    private FacebookPagesUtils() {
    }

    public static Map<String, Object> executePageRequest(Executor executor, String pageAccessToken) {
        Response response = executor
            .header(AUTHORIZATION, BEARER + " " + pageAccessToken)
            .configuration(
                Http.responseType(Http.ResponseType.JSON)
                    .disableAuthorization(true))
            .execute();

        Map<String, Object> body = response.getBody(new TypeReference<>() {});

        int statusCode = response.getStatusCode();

        if (statusCode >= 400) {
            throw new ProviderException(
                "Facebook Graph API request failed with status %d: %s".formatted(statusCode, getErrorMessage(body)));
        }

        return body;
    }

    public static List<Map<?, ?>> getDataList(Map<String, Object> body) {
        List<Map<?, ?>> dataList = new ArrayList<>();

        if (body != null && body.get(DATA) instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof Map<?, ?> map) {
                    dataList.add(map);
                }
            }
        }

        return dataList;
    }

    public static String getPageAccessToken(Context context, String pageId) {
        Map<String, Object> body = context.http(http -> http.get("/" + pageId))
            .queryParameter("fields", ACCESS_TOKEN)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body != null && body.get(ACCESS_TOKEN) instanceof String accessToken && !accessToken.isEmpty()) {
            return accessToken;
        }

        throw new ProviderException(
            "Unable to get the access token for the Facebook Page " + pageId +
                ". Make sure the connected user manages this Page.");
    }

    public static List<Option<String>> getPageIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, Object> body = context.http(http -> http.get("/me/accounts"))
            .queryParameters("fields", "id,name", "limit", 100)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        for (Map<?, ?> page : getDataList(body)) {
            options.add(option((String) page.get(NAME), (String) page.get(ID)));
        }

        return options;
    }

    private static String getErrorMessage(Map<String, Object> body) {
        if (body != null && body.get("error") instanceof Map<?, ?> error && error.get(MESSAGE) instanceof String
            message) {

            return message;
        }

        return String.valueOf(body);
    }
}
