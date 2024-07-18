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

package com.bytechef.component.box.util;

import static com.bytechef.component.box.constant.BoxConstants.BASE_URL;
import static com.bytechef.component.box.constant.BoxConstants.FILE;
import static com.bytechef.component.box.constant.BoxConstants.FOLDER;
import static com.bytechef.component.box.constant.BoxConstants.ID;
import static com.bytechef.component.box.constant.BoxConstants.NAME;
import static com.bytechef.component.box.constant.BoxConstants.TYPE;
import static com.bytechef.component.definition.ComponentDSL.option;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Monika Domiter
 */
public class BoxUtils {

    private BoxUtils() {
    }

    public static List<Option<String>> getFileIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        String parentFolder = inputParameters.getRequiredString(ID);

        Map<String, Object> body =
            context.http(http -> http.get("https://api.box.com/2.0/folders/" + parentFolder + "/items"))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get("entries") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map && Objects.equals(map.get(TYPE), FILE)) {
                    options.add(option((String) map.get(NAME), (String) map.get(ID)));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getRootFolderOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        Map<String, Object> body = context.http(http -> http.get("https://api.box.com/2.0/folders/0/items"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get("entries") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map && Objects.equals(map.get(TYPE), "folder")) {
                    options.add(option((String) map.get(NAME), (String) map.get(ID)));
                }
            }
        }

        options.add(option("ROOT", "0"));

        return options;
    }

    public static String subscribeWebhook(
        String webhookUrl, Context context, String type, String triggerEvent, String targetId) {

        Map<String, ?> body = context.http(http -> http.post(BASE_URL + "/webhooks"))
            .body(Http.Body.of(
                "address", webhookUrl,
                "triggers", List.of(triggerEvent),
                "target", Map.of(
                    ID, targetId,
                    TYPE, type)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return (String) body.get(ID);
    }

    public static void unsubscribeWebhook(Parameters outputParameters, Context context) {
        context.http(http -> http.delete(BASE_URL + "/webhooks/" + outputParameters.getString(ID)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();
    }

}
