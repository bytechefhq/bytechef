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

package com.bytechef.component.vbout.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.vbout.constant.VboutConstants.CHANNEL;
import static com.bytechef.component.vbout.constant.VboutConstants.ID;
import static com.bytechef.component.vbout.constant.VboutConstants.NAME;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class VboutUtils {

    private VboutUtils() {
    }

    public static List<Option<String>> getChannelIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, Map<String, ?>> body = context
            .http(http -> http.get("/socialMedia/Channels"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        Map<String, ?> channels = body.get("channels");

        String channel = inputParameters.getRequiredString(CHANNEL);

        if (channels.get(channel) instanceof Map<?, ?> platformMap) {
            List<?> list = switch (channel) {
                case "Facebook" -> (List<?>) platformMap.get("pages");
                case "Twitter", "Linkedin" -> (List<?>) platformMap.get("profiles");
                default -> new ArrayList<>();
            };

            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    options.add(option((String) map.get(NAME), (String) map.get(ID)));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getContactIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, Map<String, ?>> body = context
            .http(http -> http.get("/emailmarketing/getcontacts"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        Map<String, ?> contacts = body.get("contacts");
        if (contacts.get("items") instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get("email"), (String) map.get(ID)));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getListIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, Map<String, ?>> body = context
            .http(http -> http.get("/emailmarketing/getlists"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        Map<String, ?> lists = body.get("lists");
        if (lists.get("items") instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get(NAME), (String) map.get(ID)));
                }
            }
        }

        return options;
    }
}
